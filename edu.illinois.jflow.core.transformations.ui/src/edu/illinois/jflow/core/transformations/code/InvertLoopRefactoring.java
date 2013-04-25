package edu.illinois.jflow.core.transformations.code;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

/**
 * Inverts the closures in a loop statement to use our custom FlowGraph dataflow operator. This
 * custom FlowGraph class is meant to make it easy to create fork/join tasks.
 * 
 * @author nchen
 * 
 */
@SuppressWarnings("restriction")
public class InvertLoopRefactoring extends Refactoring {

	class HoistedClosureCreator {
		// Though using string templates might seems like a foolish idea, it is much more succinct that building
		// this entire thing programmatically. This is true whenever we are doing more code generation than just manipulation
		// e.g. replace or modify.
		static final String FLOWGRAPH_OPERATOR_TEMPLATE= FLOWGRAPH_VARIABLE_NAME + ".operator(Arrays.asList(%s), Arrays.asList(%s), %s);";

		private int stageName;

		private ClassInstanceCreation instanceCreation;

		public HoistedClosureCreator(int stageName, ClassInstanceCreation instanceCreation) {
			this.stageName= stageName;
			this.instanceCreation= instanceCreation;
		}

		Statement generateHoistedStatement() {
			String hoistedStatement= String.format(FLOWGRAPH_OPERATOR_TEMPLATE, getInputChannel(), getOutputChannel(), extractDataflowMessasingRunnableClass());
			return (Statement)ASTNodeFactory.newStatement(fAST, hoistedStatement);
		}

		// We just need to extract the anonymous inner class and stick it into the operator statement
		private String extractDataflowMessasingRunnableClass() {
			return instanceCreation.toString();
		}

		private String getInputChannel() {
			int inputChannelNumber= stageName - 1;
			return ExtractClosureRefactoring.GENERIC_CHANNEL_NAME + inputChannelNumber;
		}

		private String getOutputChannel() {
			int size= fAnalyzer.getClosureStatements().size();
			if (stageName != size)
				return ExtractClosureRefactoring.GENERIC_CHANNEL_NAME + stageName;
			else
				return ""; // Empty list of next output channel //$NON-NLS-1$
		}
	}

	private ICompilationUnit fCUnit;

	private CompilationUnit fRoot;

	private int fSelectionStart;

	private int fSelectionLength;

	private ImportRewrite fImportRewriter;

	private AST fAST;

	private InvertLoopAnalyzer fAnalyzer;

	private ASTRewrite fRewriter;

	// This section is specific to the API for GPars Dataflow

	private static final String FLOWGRAPH_PACKAGE= "groovyx.gpars.dataflow.operator";

	private static final String FLOWGRAPH_TYPE= "FlowGraph";

	private static final String FLOWGRAPH_QUALIFIED_TYPE= FLOWGRAPH_PACKAGE + "." + FLOWGRAPH_TYPE;

	private static final String ARRAYS_TYPE= "java.util.Arrays";

	private static final String FLOWGRAPH_VARIABLE_NAME= "fGraph";

	private static final String WAITFORALL_METHOD= "waitForAll()";

	public InvertLoopRefactoring(ICompilationUnit unit, int selectionStart, int selectionLength) {
		fCUnit= unit;
		fRoot= null;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
	}

	@Override
	public String getName() {
		return JFlowRefactoringCoreMessages.InvertLoopRefactoring_name;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		pm.beginTask("", 100); //$NON-NLS-1$

		IFile[] changedFiles= ResourceUtil.getFiles(new ICompilationUnit[] { fCUnit });
		status.merge(Checks.validateModifiesFiles(changedFiles, getValidationContext()));
		if (status.hasFatalError())
			return status;

		if (fRoot == null) {
			fRoot= RefactoringASTParser.parseWithASTProvider(fCUnit, true, new SubProgressMonitor(pm, 99));
		}
		fImportRewriter= StubUtility.createImportRewrite(fRoot, true);
		fAST= fRoot.getAST();

		fRoot.accept(createVisitor(pm));

		fSelectionStart= fAnalyzer.getSelection().getOffset();
		fSelectionLength= fAnalyzer.getSelection().getLength();

		status.merge(fAnalyzer.checkInitialConditions(fImportRewriter));
		if (status.hasFatalError())
			return status;

		return status;
	}

	private ASTVisitor createVisitor(IProgressMonitor pm) throws CoreException {
		fAnalyzer= new InvertLoopAnalyzer(Selection.createFromStartLength(fSelectionStart, fSelectionLength), fRoot, pm);
		return fAnalyzer;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask("", 2); //$NON-NLS-1$
		try {
			BodyDeclaration declaration= fAnalyzer.getEnclosingBodyDeclaration();
			fRewriter= ASTRewrite.create(declaration.getAST());

			final CompilationUnitChange result= new CompilationUnitChange(JFlowRefactoringCoreMessages.ExtractClosureRefactoring_change_name, fCUnit);
			result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

			MultiTextEdit root= new MultiTextEdit();
			result.setEdit(root);

			setupFlowGraph(result);
			hoistClosures(result);

			// IMPORTS
			//////////

			fImportRewriter.addImport(ARRAYS_TYPE);
			fImportRewriter.addImport(FLOWGRAPH_QUALIFIED_TYPE);
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

	private void setupFlowGraph(final CompilationUnitChange result) {
		TextEditGroup createFlowGraphDesc= new TextEditGroup(JFlowRefactoringCoreMessages.InvertLoopRefactoring_introduce_flowgraph_textedit_description);
		result.addTextEditGroup(createFlowGraphDesc);

		Statement forStatement= locateEnclosingLoopStatement();
		ChildListPropertyDescriptor forStatementDescriptor= (ChildListPropertyDescriptor)forStatement.getLocationInParent();
		ListRewrite forStatementListRewrite= fRewriter.getListRewrite(forStatement.getParent(), forStatementDescriptor);

		String newFlowGraphStatement= String.format("%s %s = new %s();", FLOWGRAPH_TYPE, FLOWGRAPH_VARIABLE_NAME, FLOWGRAPH_TYPE);
		ASTNode newStatement= ASTNodeFactory.newStatement(fAST, newFlowGraphStatement);
		forStatementListRewrite.insertBefore(newStatement, forStatement, createFlowGraphDesc);

		String waitFlowGraphStatement= String.format("%s.%s;", FLOWGRAPH_VARIABLE_NAME, WAITFORALL_METHOD);
		ASTNode waitStatement= ASTNodeFactory.newStatement(fAST, waitFlowGraphStatement);
		forStatementListRewrite.insertAfter(waitStatement, forStatement, createFlowGraphDesc);
	}

	private void hoistClosures(final CompilationUnitChange result) {
		TextEditGroup hoistClosureDesc= new TextEditGroup(JFlowRefactoringCoreMessages.InvertLoopRefactoring_hoist_closure_textedit_description);
		result.addTextEditGroup(hoistClosureDesc);

		Statement forStatement= locateEnclosingLoopStatement();
		ChildListPropertyDescriptor forStatementDescriptor= (ChildListPropertyDescriptor)forStatement.getLocationInParent();
		ListRewrite forStatementListRewrite= fRewriter.getListRewrite(forStatement.getParent(), forStatementDescriptor);

		// Hoist the old closures from the loop body - iterate in reverse order so we can add them in order using
		// the for loop as a pivot point
		List<ExpressionStatement> closuresExpressions= fAnalyzer.getClosureStatements();
		List<ClassInstanceCreation> closureInstances= fAnalyzer.getClosureInstantiations();
		for (int stage= 0; stage < closuresExpressions.size(); stage++) {
			// Remove old closure
			ExpressionStatement node= closuresExpressions.get(stage);
			fRewriter.remove(node, hoistClosureDesc);

			// Insert newly hoistedClosure
			HoistedClosureCreator hc= new HoistedClosureCreator(stage + 1, closureInstances.get(stage));
			Statement hoistedStatement= hc.generateHoistedStatement();
			forStatementListRewrite.insertBefore(hoistedStatement, forStatement, hoistClosureDesc);

		}
	}

	private Statement locateEnclosingLoopStatement() {
		NodeFinder nodeFinder= new NodeFinder(fRoot, fSelectionStart, fSelectionLength);
		ASTNode node= nodeFinder.getCoveringNode();
		do {
			node= node.getParent();
		} while (node != null && !(node instanceof EnhancedForStatement || node instanceof ForStatement));
		return (Statement)node;
	}
}
