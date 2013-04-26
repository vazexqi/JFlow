/**
 * This class derives
 * from {@link org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring} and is
 * licensed under the Eclipse Public License.
 */
package edu.illinois.jflow.core.transformations.code;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.StatementRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.translator.jdt.JDTIdentityMapper;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.debug.Assertions;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGExtractClosureAnalyzer;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGPartitionerChecker;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;
import edu.illinois.jflow.wala.utils.EclipseProjectAnalysisEngine;

/**
 * Extracts a closure in a compilation unit based on a text selection range. The text selection
 * range must be within a FOR loop and contain annotations of the form // Begin StageX and // End
 * StageX for the analysis to work.
 * 
 * @author nchen
 */
@SuppressWarnings("restriction")
public class ExtractClosureRefactoring extends Refactoring {

	/**
	 * A structure to group together all the useful information for each stage. This structure is
	 * primarily focused only for the transformation component. The analysis component uses a
	 * different scheme to facilitate analysis and testing (less dependency on JDT).
	 * 
	 * @author nchen
	 * 
	 */
	private final class Stage {
		// Though using string templates might seems like a foolish idea, it is much more succinct that building
		// this entire thing programmatically. This is true whenever we are doing more code generation than just manipulation
		// e.g. replace or modify.
		static final String DATAFLOW_MESSAGING_RUNNABLE_TEMPLATE= "new DataflowMessagingRunnable(1) {\n" +
				"	@Override\n" +
				"	protected void doRun(Object... args) { \n" +
				"	  try {\n" +
				"			%s \n" +
				"		} catch (Exception e) {}\n" +
				"	} \n" +
				"}.call(channel%s.getVal());";

		final String NEWLINE= String.format("%n"); //$NON-NLS-1$

		final AnnotatedStage stage;

		final ExtractClosureAnalyzer analyzer;

		PDGExtractClosureAnalyzer pdgAnalyzer;

		List<ParameterInfo> parameterInfo;

		Stage(AnnotatedStage stage, ExtractClosureAnalyzer analyzer) {
			this.stage= stage;
			this.analyzer= analyzer;
		}

		ExtractClosureAnalyzer getAnalyzer() {
			return analyzer;
		}

		AnnotatedStage getStage() {
			return stage;
		}

		void setPdgAnalyzer(PDGExtractClosureAnalyzer pdgAnalyzer) {
			this.pdgAnalyzer= pdgAnalyzer;
		}

		List<ParameterInfo> getParameterInfo() {
			return parameterInfo;
		}

		void initializeParameterInfos() {
			List<IVariableBinding> arguments= pdgAnalyzer.getInputBindings(analyzer.getSelectedNodes());
			parameterInfo= new ArrayList<ParameterInfo>(arguments.size());
			ASTNode root= analyzer.getEnclosingBodyDeclaration();

			ParameterInfo vararg= null;
			int index= 0;
			for (IVariableBinding argument : arguments) {
				if (argument == null)
					continue;
				VariableDeclaration declaration= ASTNodes.findVariableDeclaration(argument, root);
				boolean isVarargs= declaration instanceof SingleVariableDeclaration
						? ((SingleVariableDeclaration)declaration).isVarargs()
						: false;
				ParameterInfo info= new ParameterInfo(argument, getType(declaration, isVarargs), argument.getName(), index++);
				if (isVarargs) {
					vararg= info;
				} else {
					parameterInfo.add(info);
				}
			}
			if (vararg != null) {
				parameterInfo.add(vararg);
			}
		}

		private String getType(VariableDeclaration declaration, boolean isVarargs) {
			String type= ASTNodes.asString(ASTNodeFactory.newType(declaration.getAST(), declaration, fImportRewriter, new ContextSensitiveImportRewriteContext(declaration, fImportRewriter)));
			if (isVarargs)
				return type + ParameterInfo.ELLIPSIS;
			else
				return type;
		}

		// CODE GENERATION
		//////////////////
		public void rewriteAsDataflow(TextEditGroup description) {
			Statement dataflowStatement= createDataflowStatement();
			StatementRewrite statementRewrite= new StatementRewrite(fRewriter, analyzer.getSelectedNodes());
			statementRewrite.replace(new ASTNode[] { dataflowStatement }, description);
		}

		private Statement createDataflowStatement() {
			StringBuilder sb= new StringBuilder();

			// 1. Grab the values from the channel
			String initializationStmts= createInitializationStatements();
			sb.append(initializationStmts);

			sb.append(NEWLINE);
			// 2. Include the original statements
			for (ASTNode astNode : analyzer.getSelectedNodes()) {
				sb.append(ASTNodes.asString(astNode));
				sb.append(NEWLINE);
			}

			// 3. Pump new value into the channel - only if not the last channel
			// XXX: Revisit this assumption: what if we have a value that we want to read outside the loop?
			if (stage.stageName != stages.size()) {
				String updateStmts= createUpdateStatements();
				sb.append(updateStmts);
			}

			String closureInvocation= String.format(DATAFLOW_MESSAGING_RUNNABLE_TEMPLATE, sb.toString(), stage.stageName - 1);
			return (Statement)ASTNodeFactory.newStatement(fAST, closureInvocation);
		}

		private String createInitializationStatements() {
			StringBuilder sb= new StringBuilder();

			sb.append(String.format("%s b = ((%s) args[0]);%n", BundleCreator.BUNDLE_CLASS_NAME, BundleCreator.BUNDLE_CLASS_NAME)); //$NON-NLS-1$

			String template= "%s %s = b.%s;%n"; //$NON-NLS-1$

			for (ParameterInfo pInfo : parameterInfo) {
				sb.append(String.format(template, resolveType(pInfo.getOldBinding()), pInfo.getOldName(), pInfo.getOldName()));
			}

			return sb.toString();
		}

		private String createUpdateStatements() {
			StringBuilder sb= new StringBuilder();

			String template= "b.%s = %s;%n"; //$NON-NLS-1$

			List<IVariableBinding> arguments= pdgAnalyzer.getOutputBindings(analyzer.getSelectedNodes());
			for (IVariableBinding binding : arguments) {
				sb.append(String.format(template, binding.getName(), binding.getName()));
			}

			sb.append(String.format(GENERIC_CHANNEL_NAME + "%d.bind(b);%n", stage.stageName)); //$NON-NLS-1$

			return sb.toString();
		}

		public Collection<IVariableBinding> getOutputs() {
			return pdgAnalyzer.getOutputBindings(analyzer.getSelectedNodes());
		}

		public Collection<IVariableBinding> getInputs() {
			return pdgAnalyzer.getInputBindings(analyzer.getSelectedNodes());
		}
	}

	/**
	 * Creates a "bundle" class for all the variables that we need to pass between stages.
	 * 
	 * @author nchen
	 * 
	 */
	final class BundleCreator {
		static final String BUNDLE_CLASS_NAME= "Bundle"; //$NON-NLS-1$

		Set<ParameterInfo> variables= new TreeSet<ParameterInfo>(new ParameterInfoComparator());

		BundleCreator(Collection<Stage> stages) {
			// Initialize all the variables that we would need
			for (Stage stage : stages) {
				variables.addAll(stage.getParameterInfo());
			}
		}

		AbstractTypeDeclaration createNewNestedClass() {
			TypeDeclaration bundleClass= fAST.newTypeDeclaration();
			bundleClass.setName(fAST.newSimpleName(BUNDLE_CLASS_NAME));

			@SuppressWarnings("unchecked")
			List<BodyDeclaration> bodyDeclarations= bundleClass.bodyDeclarations();

			for (ParameterInfo pInfo : variables) {
				VariableDeclarationFragment fragment= fAST.newVariableDeclarationFragment();
				fragment.setName(fAST.newSimpleName(pInfo.getOldName()));

				FieldDeclaration field= fAST.newFieldDeclaration(fragment);
				IVariableBinding varType= pInfo.getOldBinding();
				field.setType(fAST.newSimpleType(fAST.newSimpleName(resolveType(varType))));

				bodyDeclarations.add(field);
			}

			return bundleClass;
		}
	}

	/**
	 * <p>
	 * This class calculates the values that must be "pumped" into the loop at the begining.
	 * </p>
	 * <p>
	 * It does this very simply:
	 * <ol>
	 * <li>Get all the stages involved and create a set of all the variables that are being
	 * produced. Call this set P.</li>
	 * <li>Get all the stages involved and create a set of all variables that are being consumed.
	 * Call this set C.</li>
	 * <li>The set difference C \ P gives us the variables that are not produced by any of the
	 * stages. Thus, those variables must come from outside and must be "pumped" into the loop.</li>
	 * </ol>
	 * </p>
	 * 
	 * @author nchen
	 * 
	 */
	final class PrologueCreator {

		private static final String BUNDLE_VARIABLE_NAME= "b";

		private static final String INITIAL_CHANNEL= GENERIC_CHANNEL_NAME + "0";

		Set<IVariableBinding> produced= new TreeSet<IVariableBinding>(new IVariableBindingComparator());

		Set<IVariableBinding> consumed= new TreeSet<IVariableBinding>(new IVariableBindingComparator());

		PrologueCreator(Collection<Stage> stages) {
			// Initialize all the variables that we would need
			for (Stage stage : stages) {
				produced.addAll(stage.getOutputs());
				consumed.addAll(stage.getInputs());
			}
		}

		List<Statement> createInitializationStatements() {
			List<Statement> statements= new ArrayList<Statement>();

			consumed.removeAll(produced); // Whatever remains - should be at least size 1 since the loop produces at least one variable

			if (consumed.size() > 0) { // This should always be true
				// Declaration statement
				Statement declStatement= (Statement)ASTNodeFactory.newStatement(fAST,
						String.format("%s %s = new %s();", BundleCreator.BUNDLE_CLASS_NAME, BUNDLE_VARIABLE_NAME, BundleCreator.BUNDLE_CLASS_NAME));
				statements.add(declStatement);

				// Initialization statement
				String template= BUNDLE_VARIABLE_NAME + ".%s = %s;";
				for (IVariableBinding iVar : consumed) {
					Statement initializationStmt= (Statement)ASTNodeFactory.newStatement(fAST, String.format(template, iVar.getName(), iVar.getName()));
					statements.add(initializationStmt);
				}

				// Pump into the channel
				Statement pump= (Statement)ASTNodeFactory.newStatement(fAST, String.format("%s.bind(%s);", INITIAL_CHANNEL, BUNDLE_VARIABLE_NAME));
				statements.add(pump);
			}

			return statements;
		}
	}

	/**
	 * Distinguish the different parameter info from each stage by their names. While using names
	 * might sound absurd, recall that we can safely make the assumption that because that all the
	 * stages are within the same block, the names are indeed unique and can be used to
	 * differentiate.
	 * 
	 * @author nchen
	 * 
	 */
	private final class ParameterInfoComparator implements Comparator<ParameterInfo> {
		@Override
		public int compare(ParameterInfo pInfoLeft, ParameterInfo pInfoRight) {
			return pInfoLeft.getOldName().compareTo(pInfoRight.getOldName());
		}
	}

	/**
	 * Distinguishes different variable bindings by getKey() which is guaranteed to be unique.
	 * 
	 * @author nchen
	 * 
	 */
	private final class IVariableBindingComparator implements Comparator<IVariableBinding> {
		@Override
		public int compare(IVariableBinding iVarLeft, IVariableBinding iVarRight) {
			return iVarLeft.getKey().compareTo(iVarRight.getKey());
		}
	}

	private String resolveType(IVariableBinding binding) {
		ITypeBinding type= binding.getType();
		return type.getName();
	}

	private ICompilationUnit fCUnit;

	private CompilationUnit fRoot;

	private AST fAST;

	private ImportRewrite fImportRewriter;

	private int fSelectionStart;

	private int fSelectionLength;

	private ASTRewrite fRewriter;

	private Map<Integer, Stage> stages;

	private IDocument fDoc;

	private PDGPartitionerChecker fChecker;

	// GPARS
	////////

	public static final String DATAFLOWQUEUE_TYPE= "groovyx.gpars.dataflow.DataflowQueue"; //$NON-NLS-1$

	public static final String GENERIC_CHANNEL_NAME= "channel"; //$NON-NLS-1$

	public static final String CLOSURE_METHOD= "doRun"; //$NON-NLS-1$

	public static final String CLOSURE_TYPE= "DataflowMessagingRunnable"; //$NON-NLS-1$

	public static final String CLOSURE_PACKAGE= "groovyx.gpars"; //$NON-NLS-1$

	public static final String DATAFLOWMESSAGING_TYPE= CLOSURE_PACKAGE + "." + CLOSURE_TYPE; //$NON-NLS-1$

	public static final String DATAFLOWQUEUE_INTERFACE= "groovyx.gpars.dataflow.DataflowChannel"; //$NON-NLS-1$

	private AbstractAnalysisEngine fEngine;

	private CallGraph fCallGraph;

	/**
	 * Creates a new extract closure refactoring
	 * 
	 * @param unit the compilation unit
	 * @param doc the document of the current editor
	 * @param selectionStart selection start
	 * @param selectionLength selection end
	 */
	public ExtractClosureRefactoring(ICompilationUnit unit, IDocument doc, int selectionStart, int selectionLength) {
		fCUnit= unit;
		fDoc= doc;
		fRoot= null;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		stages= new HashMap<Integer, Stage>();
	}

	@Override
	public String getName() {
		return JFlowRefactoringCoreMessages.ExtractClosureRefactoring_name;
	}

	// INITIALIZATION OF ANALYZERS
	//////////////////////////////

	/**
	 * Checks if the refactoring can be activated.
	 * 
	 * @param pm a progress monitor to report progress during activation checking.
	 * @return the refactoring status describing the result of the activation check.
	 * @throws CoreException if checking fails
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 100); //$NON-NLS-1$

		IFile[] changedFiles= ResourceUtil.getFiles(new ICompilationUnit[] { fCUnit });
		result.merge(Checks.validateModifiesFiles(changedFiles, getValidationContext()));
		if (result.hasFatalError())
			return result;
		result.merge(ResourceChangeChecker.checkFilesToBeChanged(changedFiles, new SubProgressMonitor(pm, 1)));

		if (fRoot == null) {
			fRoot= RefactoringASTParser.parseWithASTProvider(fCUnit, true, new SubProgressMonitor(pm, 99));
		}
		fImportRewriter= StubUtility.createImportRewrite(fRoot, true);
		fAST= fRoot.getAST();

		// This is some light-weight analyzer that checks for control flow
		initializeStages(result);

		if (result.hasFatalError())
			return result;

		// If we don't have any errors at this point, we can initialize the heavy-lifting parts
		initializeStageAnalyzers(result);

		// DEBUGGING
		for (int stageNumber= 0; stageNumber < stages.values().size(); stageNumber++) {
			System.out.println(String.format("STAGE: %d%n", stageNumber)); //$NON-NLS-1$
			for (ParameterInfo info : stages.get(stageNumber).getParameterInfo()) {
				System.out.println(info);
			}
			System.out.println(String.format("%n%n")); //$NON-NLS-1$
		}
		return result;
	}

	private void initializeStageAnalyzers(RefactoringStatus result) {
		try {
			initializePDGAnalyzers();

			if (fChecker.containsLoopCarriedDependency()) {
				result.addError("There is a loop carried dependency and we cannot parallelize this loop.");
			}

			fChecker.computeHeapDependency(fCallGraph, fEngine.getPointerAnalysis());
			fChecker.checkInterference();

			if (fChecker.hasInteference()) {
				List<String> messages= fChecker.getInterferenceMessages();
				for (String message : messages) {
					result.addWarning(message);
				}
			}

			List<String> ignoredMethodCalls= fChecker.getIgnoredMethodCalls();
			if (!ignoredMethodCalls.isEmpty()) {
				for (String ignored : ignoredMethodCalls) {
					result.addWarning(ignored);
				}
			}


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeStages(RefactoringStatus result) throws CoreException {
		MethodDeclaration methodDeclaration= locateSelectedMethod();
		AnnotatedStagesFinder locator= new AnnotatedStagesFinder(fRoot, fDoc, methodDeclaration);

		List<AnnotatedStage> annotatedStages= locator.locateStages();

		// We do not fuse these two loops since we want to initialize all of them first before
		// reporting any errors

		for (int stageNumber= 0; stageNumber < annotatedStages.size(); stageNumber++) {
			AnnotatedStage stage= annotatedStages.get(stageNumber);
			ExtractClosureAnalyzer analyzer= new ExtractClosureAnalyzer(fCUnit, stage.getSelection());
			Stage stageInfo= new Stage(stage, analyzer);
			stages.put(stageNumber, stageInfo);
		}

		for (Stage stage : stages.values()) {
			ExtractClosureAnalyzer analyzer= stage.getAnalyzer();
			fRoot.accept(analyzer);
			result.merge(analyzer.checkInitialConditions());
		}
	}

	private void initializePDGAnalyzers() throws IOException, CoreException, InvalidClassFileException, IllegalArgumentException, CancelException {
		fEngine= new EclipseProjectAnalysisEngine(fCUnit.getJavaProject());
		fCallGraph= fEngine.buildDefaultCallGraph();

		// Get the IR for the selected method
		// Since all the stages are going to be in the same method, just use the first ExtractClosureAnalyzer
		MethodDeclaration methodDeclaration= locateSelectedMethod();
		JDTIdentityMapper mapper= new JDTIdentityMapper(JavaSourceAnalysisScope.SOURCE, fAST);
		MethodReference methodRef= mapper.getMethodRef(methodDeclaration.resolveBinding());

		Set<CGNode> nodes= fCallGraph.getNodes(methodRef);

		Assertions.productionAssertion(nodes.size() == 1, "Expected a single corresponding CGNode, but got either 0 or more");

		CGNode node= nodes.iterator().next(); // Quick way to get first element of set with single entry since set doesn't implement get();
		IR ir= node.getIR();

		ProgramDependenceGraph pdg= ProgramDependenceGraph.makeWithSourceCode(ir, fEngine.getClassHierarchy(), fDoc);

		initializePDGExtractClosureAnalyzers(pdg);
		initializePDGPartitionChecker(pdg);
	}


	private void initializePDGExtractClosureAnalyzers(ProgramDependenceGraph pdg) {
		for (int stageNumber= 0; stageNumber < stages.keySet().size(); stageNumber++) {
			Stage stage= stages.get(stageNumber);
			PDGExtractClosureAnalyzer pdgAnalyzer= new PDGExtractClosureAnalyzer(pdg, stageNumber, stage.getStage().getStageLines());
			stage.setPdgAnalyzer(pdgAnalyzer);
			pdgAnalyzer.analyzeSelection();
			stage.initializeParameterInfos();
		}
	}

	private void initializePDGPartitionChecker(ProgramDependenceGraph pdg) {
		List<List<Integer>> selections= new ArrayList<List<Integer>>();

		selections.add(getEnclosingLoopLines());
		for (int stageNumber= 0; stageNumber < stages.keySet().size(); stageNumber++) {
			Stage stage= stages.get(stageNumber);
			selections.add(stage.getStage().getStageLines());
		}

		fChecker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);
	}

	// LOCATING NODES
	//////////////////

	private MethodDeclaration locateSelectedMethod() {
		NodeFinder nodeFinder= new NodeFinder(fRoot, fSelectionStart, fSelectionLength);
		ASTNode coveringNode= nodeFinder.getCoveringNode();
		MethodDeclaration methodDeclaration= (MethodDeclaration)ASTNodes.getParent(coveringNode, MethodDeclaration.class);
		return methodDeclaration;
	}

	private Statement locateEnclosingLoopStatement() {
		NodeFinder nodeFinder= new NodeFinder(fRoot, fSelectionStart, fSelectionLength);
		ASTNode node= nodeFinder.getCoveringNode();
		do {
			node= node.getParent();
		} while (node != null && !(node instanceof EnhancedForStatement || node instanceof ForStatement));
		return (Statement)node;
	}

	List<Integer> getEnclosingLoopLines() {
		List<Integer> lines= new ArrayList<Integer>();
		Statement forLoop= locateEnclosingLoopStatement();
		// IDocument starts counting from 0 but we want to follow what the user sees in the editor
		// that starts from 1.
		try {
			int start= fDoc.getLineOfOffset(forLoop.getStartPosition()) + 1;
			int end= fDoc.getLineOfOffset(forLoop.getStartPosition() + forLoop.getLength()) + 1;
			for (int line= start; line <= end; line++) {
				lines.add(line);
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return lines;
	}

	/* (non-Javadoc)
	 * Method declared in Refactoring
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		return result;
	}

	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$


		try {
			fRewriter= ASTRewrite.create(fAST);

			final CompilationUnitChange result= new CompilationUnitChange(JFlowRefactoringCoreMessages.ExtractClosureRefactoring_change_name, fCUnit);
			result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

			MultiTextEdit root= new MultiTextEdit();
			result.setEdit(root);

			// 1. Create the bundle class
			createMethodBundle(result);
			// 2. Create the channels
			createChannels(result);

			// 3. Replace the original statements with DataflowMessagingRunnable closures
			TextEditGroup replaceOriginalWithDataflowDesc= new TextEditGroup(JFlowRefactoringCoreMessages.ExtractClosureRefactoring_replace_statement_textedit_description);
			result.addTextEditGroup(replaceOriginalWithDataflowDesc);

			insertPrologueStatements(replaceOriginalWithDataflowDesc);
			replaceStatements(replaceOriginalWithDataflowDesc);

			// IMPORTS
			//////////

			fImportRewriter.addImport(DATAFLOWQUEUE_TYPE);
			fImportRewriter.addImport(DATAFLOWMESSAGING_TYPE);

			if (fImportRewriter.hasRecordedChanges()) {
				TextEdit edit= fImportRewriter.rewriteImports(null);
				root.addChild(edit);
				result.addTextEditGroup(new TextEditGroup(JFlowRefactoringCoreMessages.ExtractClosureRefactoring_organize_imports, new TextEdit[] { edit }));
			}
			root.addChild(fRewriter.rewriteAST());
			return result;
		} finally {
			pm.done();
		}
	}

	private void createMethodBundle(final CompilationUnitChange result) {
		BodyDeclaration methodDecl= locateSelectedMethod();

		TextEditGroup insertClassDesc= new TextEditGroup(JFlowRefactoringCoreMessages.ExtractClosureRefactoring_bundle_textedit_description);
		result.addTextEditGroup(insertClassDesc);

		BundleCreator bc= new BundleCreator(stages.values());
		AbstractTypeDeclaration newClass= bc.createNewNestedClass();

		ChildListPropertyDescriptor methodDeclDescriptor= (ChildListPropertyDescriptor)methodDecl.getLocationInParent();
		ListRewrite methodDeclContainer= fRewriter.getListRewrite(methodDecl.getParent(), methodDeclDescriptor);
		methodDeclContainer.insertBefore(newClass, methodDecl, insertClassDesc);
	}

	private void createChannels(final CompilationUnitChange result) {
		Statement forStatement= locateEnclosingLoopStatement();

		TextEditGroup insertChannelDesc= new TextEditGroup(JFlowRefactoringCoreMessages.ExtractClosureRefactoring_channel_textedit_description);
		result.addTextEditGroup(insertChannelDesc);

		List<Statement> channelStatements= createChannelStatements();

		ChildListPropertyDescriptor forStatementDescriptor= (ChildListPropertyDescriptor)forStatement.getLocationInParent();
		ListRewrite forStatementListRewrite= fRewriter.getListRewrite(forStatement.getParent(), forStatementDescriptor);

		// We use the loop at the anchor point and insert everything before it in alphabetical channel name order
		for (Statement stmt : channelStatements) {
			forStatementListRewrite.insertBefore(stmt, forStatement, insertChannelDesc);
		}
	}

	private List<Statement> createChannelStatements() {
		List<Statement> channelStatements= new ArrayList<Statement>();

		for (int i= 0; i < stages.size(); i++) {
			String channelDeclarationStatement= String.format("final DataflowQueue<%s> %s%d = new DataflowQueue<%s>();", BundleCreator.BUNDLE_CLASS_NAME, GENERIC_CHANNEL_NAME, i, //$NON-NLS-1$
					BundleCreator.BUNDLE_CLASS_NAME);
			Statement newStatement= (Statement)ASTNodeFactory.newStatement(fAST, channelDeclarationStatement);
			channelStatements.add(newStatement);
		}

		return channelStatements;
	}

	private void insertPrologueStatements(TextEditGroup replaceOriginalWithDataflowDesc) {
		Statement statement= locateEnclosingLoopStatement();
		ListRewrite loopRewriter= null;
		Statement body= null;

		// TODO: Handle other kinds of loops
		if (statement instanceof ForStatement) {
			ForStatement forStatement= (ForStatement)statement;
			body= forStatement.getBody();
		} else if (statement instanceof EnhancedForStatement) {
			EnhancedForStatement enhancedForStatement= (EnhancedForStatement)statement;
			body= enhancedForStatement.getBody();
		}

		if (body != null) {
			loopRewriter= fRewriter.getListRewrite(body, Block.STATEMENTS_PROPERTY);
			if (loopRewriter != null) {
				PrologueCreator pc= new PrologueCreator(stages.values());
				List<Statement> statements= pc.createInitializationStatements();
				// We have to insert in reverse other since we are using the 0 position (that is what insertFirst) does
				// as the anchor point
				for (int index= statements.size() - 1; index >= 0; index--) {
					loopRewriter.insertFirst(statements.get(index), replaceOriginalWithDataflowDesc);
				}
			}
		}
	}

	private void replaceStatements(TextEditGroup replaceOriginalWithDataflowDesc) {
		for (Stage stage : stages.values()) {
			stage.rewriteAsDataflow(replaceOriginalWithDataflowDesc);
		}
	}
}
