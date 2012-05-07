package edu.illinois.jflow.core.transformations.code;

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
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
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
 * Inverts the closures in a loop statement to use GPars Dataflow Operator
 * 
 * @author Nicholas Chen
 * 
 */
@SuppressWarnings("restriction")
public class InvertLoopRefactoring extends Refactoring {


	private ICompilationUnit fCUnit;

	private CompilationUnit fRoot;

	private int fSelectionStart;

	private int fSelectionLength;

	private ImportRewrite fImportRewriter;

	private AST fAST;

	private InvertLoopAnalyzer fAnalyzer;

	private ASTRewrite fRewriter;

	// This section is specific to the API for GPars Dataflow

	private static final String FLOWGRAPH_TYPE= "groovyx.gpars.dataflow.operator.FlowGraph";

	public InvertLoopRefactoring(ICompilationUnit unit, int selectionStart, int selectionLength) {
		fCUnit= unit;
		fRoot= null;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
	}

	public InvertLoopRefactoring(CompilationUnit astRoot, int selectionStart, int selectionLength) {
		this((ICompilationUnit)astRoot.getTypeRoot(), selectionStart, selectionLength);
		fRoot= astRoot;
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
		fAnalyzer= new InvertLoopAnalyzer(Selection.createFromStartLength(fSelectionStart, fSelectionLength), pm);
		return fAnalyzer;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();

		//TODO: Do we need any additional inputs from the user?

		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		pm.beginTask("", 2);
		try {
			BodyDeclaration declaration= fAnalyzer.getEnclosingBodyDeclaration();
			fRewriter= ASTRewrite.create(declaration.getAST());

			final CompilationUnitChange result= new CompilationUnitChange(JFlowRefactoringCoreMessages.ExtractClosureRefactoring_change_name, fCUnit);
			result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

			MultiTextEdit root= new MultiTextEdit();
			result.setEdit(root);

			TextEditGroup inverterEditGroup= new TextEditGroup("Invert Loop");
			result.addTextEditGroup(inverterEditGroup);

			ASTNode selectedLoopStatement= fAnalyzer.getSelectedLoopStatement();
			ListRewrite rewriter= fRewriter.getListRewrite(selectedLoopStatement.getParent(), (ChildListPropertyDescriptor)selectedLoopStatement.getLocationInParent());

			createFlowGraph(selectedLoopStatement, rewriter, inverterEditGroup);
			createChannels(selectedLoopStatement, rewriter, inverterEditGroup);
			hoiseClosures(selectedLoopStatement, rewriter, inverterEditGroup);

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

	private void createFlowGraph(ASTNode selectedLoopStatement, ListRewrite rewriter, TextEditGroup inverterEditGroup) {
		//Use string generation since this is a single statement
		String flowGraph= "FlowGraph fGraph = new FlowGraph();";
		ASTNode newStatement= ASTNodeFactory.newStatement(fAST, flowGraph);
		rewriter.insertBefore(newStatement, selectedLoopStatement, inverterEditGroup);
		fImportRewriter.addImport(FLOWGRAPH_TYPE);
	}

	private void hoiseClosures(ASTNode selectedLoopStatement, ListRewrite rewriter, TextEditGroup inverterEditGroup) {
		for (ClassInstanceCreation closure : fAnalyzer.getClosures()) {
			ASTNode instanceCreationTarget= fRewriter.createMoveTarget(closure);
			rewriter.insertBefore(instanceCreationTarget, selectedLoopStatement, inverterEditGroup);
		}
	}

	private void createChannels(ASTNode selectedLoopStatement, ListRewrite rewriter, TextEditGroup inverterEditGroup) {
		// Create new channel for loop
		ASTNode newStatement= null;
		if (selectedLoopStatement instanceof EnhancedForStatement) {
			EnhancedForStatement forLoop= (EnhancedForStatement)selectedLoopStatement;
			SingleVariableDeclaration parameter= forLoop.getParameter();
			String name= parameter.getType().toString();
			String channel= "final DataflowQueue<" + name + "> " + ExtractClosureRefactoring.GENERIC_CHANNEL_NAME + "0" + "= new DataflowQueue<" + name + ">();";
			newStatement= ASTNodeFactory.newStatement(fAST, channel);
		}

		rewriter.insertBefore(newStatement, selectedLoopStatement, inverterEditGroup);

		hoiseExistingChannels(selectedLoopStatement, rewriter, inverterEditGroup);
	}

	private void hoiseExistingChannels(ASTNode selectedLoopStatement, ListRewrite rewriter, TextEditGroup inverterEditGroup) {
		for (VariableDeclarationStatement variableDeclarationStatement : fAnalyzer.getChannels()) {
			ASTNode variableDeclarationTarget= fRewriter.createMoveTarget(variableDeclarationStatement);
			rewriter.insertBefore(variableDeclarationTarget, selectedLoopStatement, inverterEditGroup);
		}
	}
}
