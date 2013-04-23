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
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
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
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGExtractClosureAnalyzer;
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
		final AnnotatedStage stage;

		final ExtractClosureAnalyzer analyzer;

		PDGExtractClosureAnalyzer pdgAnalyzer;

		List<ParameterInfo> parameterInfo;

		Stage(AnnotatedStage stage, ExtractClosureAnalyzer analyzer) {
			this.stage= stage;
			this.analyzer= analyzer;
		}

		public ExtractClosureAnalyzer getAnalyzer() {
			return analyzer;
		}

		public AnnotatedStage getStage() {
			return stage;
		}

		public void setPdgAnalyzer(PDGExtractClosureAnalyzer pdgAnalyzer) {
			this.pdgAnalyzer= pdgAnalyzer;
		}

		public List<ParameterInfo> getParameterInfo() {
			return parameterInfo;
		}

		private void initializeParameterInfos() {
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
	}

	/**
	 * Creates a "bundle" class for all the variables that we need to pass between stages.
	 * 
	 * @author nchen
	 * 
	 */
	final class BundleCreator {
		static final String BUNDLE_CLASS_NAME= "Bundle"; //$NON-NLS-1$

		// Distinguish the different parameter info from each stage by their names. While using names might sound absurd,
		// recall that we can safely make the assumption that because that all the stages are within the same block, the names 
		// are indeed unique and can be used to differentiate.
		Set<ParameterInfo> variables= new TreeSet<ParameterInfo>(new Comparator<ParameterInfo>() {

			@Override
			public int compare(ParameterInfo pInfoLeft, ParameterInfo pInfoRight) {
				return pInfoLeft.getOldName().compareTo(pInfoRight.getOldName());
			}
		});

		BundleCreator(Collection<Stage> stages) {
			// Initialize all the variables that we would need
			for (Stage stage : stages) {
				variables.addAll(stage.getParameterInfo());
			}
		}

		private String resolveType(IVariableBinding binding) {
			ITypeBinding type= binding.getType();
			return type.getName();
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

	private ICompilationUnit fCUnit;

	private CompilationUnit fRoot;

	private AST fAST;

	private ImportRewrite fImportRewriter;

	private int fSelectionStart;

	private int fSelectionLength;

	private ASTRewrite fRewriter;

	private Map<Integer, Stage> stages;

	private IDocument fDoc;

	private static final String EMPTY= ""; //$NON-NLS-1$

	// GPARS
	////////

	public static final String DATAFLOWQUEUE_TYPE= "groovyx.gpars.dataflow.DataflowQueue";

	public static final String DATAFLOWMESSAGING_TYPE= "groovyx.gpars.DataflowMessagingRunnable";

	public static final String GENERIC_CHANNEL_NAME= "channel"; //$NON-NLS-1$

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
		initializeStageAnalyzers();

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

	private void initializeStageAnalyzers() {
		try {
			// XXX: Check for actual heap and dependencies
			initializePDGAnalyzers();
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
		// Set up the analysis engine
		AbstractAnalysisEngine engine= new EclipseProjectAnalysisEngine(fCUnit.getJavaProject());
		engine.buildAnalysisScope();
		final IClassHierarchy classHierarchy= engine.buildClassHierarchy();
		final AnalysisOptions options= new AnalysisOptions();
		final AnalysisCache cache= engine.makeDefaultCache();

		// Get the IR for the selected method
		// Since all the stages are going to be in the same method, just use the first ExtractClosureAnalyzer
		MethodDeclaration methodDeclaration= locateSelectedMethod();
		JDTIdentityMapper mapper= new JDTIdentityMapper(JavaSourceAnalysisScope.SOURCE, fAST);
		MethodReference methodRef= mapper.getMethodRef(methodDeclaration.resolveBinding());
		final IMethod resolvedMethod= classHierarchy.resolveMethod(methodRef);
		IR ir= cache.getSSACache().findOrCreateIR(resolvedMethod, Everywhere.EVERYWHERE, options.getSSAOptions());
		ProgramDependenceGraph pdg= ProgramDependenceGraph.makeWithSourceCode(ir, classHierarchy, fDoc);

		for (int stageNumber= 0; stageNumber < stages.keySet().size(); stageNumber++) {
			Stage stage= stages.get(stageNumber);
			PDGExtractClosureAnalyzer pdgAnalyzer= new PDGExtractClosureAnalyzer(pdg, fDoc, stage.getStage().getStageLines());
			stage.setPdgAnalyzer(pdgAnalyzer);
			pdgAnalyzer.analyzeSelection();
			stage.initializeParameterInfos();
		}
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

			createMethodBundle(result);
			createChannels(result);

//
//			ASTNode[] selectedNodes= fAnalyzer.getSelectedNodes();
//
//			TextEditGroup closureEditGroup= new TextEditGroup("Extract to Closure");
//			result.addTextEditGroup(closureEditGroup);
//
//			// A sentinel is just a placeholder to keep track of the position of insertion
//			// For this refactoring, we need to insert two things:
//			// 1) The DataflowChannels (if necessary)
//			// 2) The DataflowMessagingRunnable
//			Block sentinel= fAST.newBlock();
//			ListRewrite sentinelRewriter= fRewriter.getListRewrite(selectedNodes[0].getParent(), (ChildListPropertyDescriptor)selectedNodes[0].getLocationInParent());
//			sentinelRewriter.insertBefore(sentinel, selectedNodes[0], null);
//
//			// Add the dataflowChannels that are required
//			addDataflowChannels(declaration, sentinel, sentinelRewriter, closureEditGroup, pm);
//
//			// Update all references to values written in the closure body to read from channels
//			List<ASTNode> channels= createTempVariablesForChannels(declaration, closureEditGroup, pm);
//			for (ASTNode astNode : channels) {
//				sentinelRewriter.insertAfter(astNode, sentinel, closureEditGroup);
//			}
//
//			// Handle InterruptedException from using DataflowChannels
//			updateExceptions(declaration, closureEditGroup);
//
//			// Replace the placeholder sentinel with the actual code
//			ExpressionStatement closureInvocationStatement= createClosureInvocationStatement(declaration, selectedNodes, closureEditGroup, pm);
//			sentinelRewriter.replace(sentinel, closureInvocationStatement, closureEditGroup);
//
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
		ListRewrite forStatementContainer= fRewriter.getListRewrite(forStatement.getParent(), forStatementDescriptor);

		for (int index= 0; index < channelStatements.size(); index++) {
			if (index == 0) { // First one
				forStatementContainer.insertBefore(channelStatements.get(index), forStatement, insertChannelDesc);
			} else {
				forStatementContainer.insertBefore(channelStatements.get(index), forStatement, insertChannelDesc);
			}
		}
	}

	private List<Statement> createChannelStatements() {
		List<Statement> channelStatements= new ArrayList<Statement>();

		// Create the statements in reverse order to make it easier to insert later
		for (int i= stages.size() - 1; i >= 0; i--) {
			String channelDeclarationStatement= String.format("final DataflowQueue<%s> %s%d = new DataflowQueue<%s>();", BundleCreator.BUNDLE_CLASS_NAME, GENERIC_CHANNEL_NAME, i, //$NON-NLS-1$
					BundleCreator.BUNDLE_CLASS_NAME);
			Statement newStatement= (Statement)ASTNodeFactory.newStatement(fAST, channelDeclarationStatement);
			channelStatements.add(newStatement);
		}

		return channelStatements;
	}
}
