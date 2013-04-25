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
				return ""; // Empty list of next output channel
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

	private static final String FLOWGRAPH_OPERATOR_METHOD= "operator";

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
		pm.beginTask("", 2);
		try {
			BodyDeclaration declaration= fAnalyzer.getEnclosingBodyDeclaration();
			fRewriter= ASTRewrite.create(declaration.getAST());

			final CompilationUnitChange result= new CompilationUnitChange(JFlowRefactoringCoreMessages.ExtractClosureRefactoring_change_name, fCUnit);
			result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

			MultiTextEdit root= new MultiTextEdit();
			result.setEdit(root);

			TextEditGroup hoistClosureDesc= new TextEditGroup("Hoist closures out of loop body");
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

//
//			ASTNode selectedLoopStatement= fAnalyzer.getSelectedLoopStatement();
//			ListRewrite rewriter= fRewriter.getListRewrite(selectedLoopStatement.getParent(), (ChildListPropertyDescriptor)selectedLoopStatement.getLocationInParent());
//
//			createFlowGraph(selectedLoopStatement, rewriter, inverterEditGroup);
//			createChannels(selectedLoopStatement, rewriter, inverterEditGroup);
//			hoiseClosures(selectedLoopStatement, rewriter, inverterEditGroup);
//			replaceForLoop(selectedLoopStatement, rewriter, inverterEditGroup);
//			addWaitForAll(selectedLoopStatement, rewriter, inverterEditGroup);
//
//			rewriter.remove(selectedLoopStatement, inverterEditGroup);

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

	private Statement locateEnclosingLoopStatement() {
		NodeFinder nodeFinder= new NodeFinder(fRoot, fSelectionStart, fSelectionLength);
		ASTNode node= nodeFinder.getCoveringNode();
		do {
			node= node.getParent();
		} while (node != null && !(node instanceof EnhancedForStatement || node instanceof ForStatement));
		return (Statement)node;
	}

//	private void createFlowGraph(ASTNode selectedLoopStatement, ListRewrite rewriter, TextEditGroup inverterEditGroup) {
//		//Use string generation since this is a single statement
//		String flowGraph= "FlowGraph " + FLOWGRAPH_VARIABLE_NAME + " = new FlowGraph();";
//		ASTNode newStatement= ASTNodeFactory.newStatement(fAST, flowGraph);
//		rewriter.insertBefore(newStatement, selectedLoopStatement, inverterEditGroup);
//		fImportRewriter.addImport(FLOWGRAPH_QUALIFIED_TYPE);
//	}
//
//	private void createChannels(ASTNode selectedLoopStatement, ListRewrite rewriter, TextEditGroup inverterEditGroup) {
//		// Create new channel for loop
//		ASTNode newStatement= null;
//		if (selectedLoopStatement instanceof EnhancedForStatement) {
//			EnhancedForStatement forLoop= (EnhancedForStatement)selectedLoopStatement;
//			SingleVariableDeclaration parameter= forLoop.getParameter();
//			String name= parameter.getType().toString();
//			String channel= "final DataflowQueue<" + name + "> " + ExtractClosureRefactoring.GENERIC_CHANNEL_NAME + "0" + "= new DataflowQueue<" + name + ">();";
//			newStatement= ASTNodeFactory.newStatement(fAST, channel);
//		}
//
//		rewriter.insertBefore(newStatement, selectedLoopStatement, inverterEditGroup);
//
//		hoiseExistingChannels(selectedLoopStatement, rewriter, inverterEditGroup);
//	}
//
//	private void hoiseClosures(ASTNode selectedLoopStatement, ListRewrite rewriter, TextEditGroup inverterEditGroup) {
//		for (ClassInstanceCreation closure : fAnalyzer.getClosureExpressionStatement()) {
//			ExpressionStatement newExpressionStatement= fAST.newExpressionStatement(createClosureExpressionStatement(closure, inverterEditGroup));
//			rewriter.insertBefore(newExpressionStatement, selectedLoopStatement, inverterEditGroup);
//		}
//	}
//
//	private Expression createClosureExpressionStatement(ClassInstanceCreation closure, TextEditGroup inverterEditGroup) {
//		fImportRewriter.addImport(ARRAYS_TYPE);
//
//		MethodInvocation dataflowOperatorInvocation= fAST.newMethodInvocation();
//		dataflowOperatorInvocation.setExpression(fAST.newSimpleName(FLOWGRAPH_VARIABLE_NAME));
//		dataflowOperatorInvocation.setName(fAST.newSimpleName(FLOWGRAPH_OPERATOR_METHOD));
//
//		// This is safe because MethodInvocation.arguments() returns a list of Expression
//		@SuppressWarnings("unchecked")
//		List<Expression> arguments= (List<Expression>)dataflowOperatorInvocation.arguments();
//		InOutChannelInformation inOutChannelInformation= fAnalyzer.getClosures2channels().get(closure);
//
//		arguments.add(createInputChannelExpression(inOutChannelInformation.inputs));
//		arguments.add(createOutputChannelExpression(inOutChannelInformation.outputs));
//		createClosureArgument(arguments, closure, inOutChannelInformation.outputs, inverterEditGroup);
//
//		return dataflowOperatorInvocation;
//	}
//
//	private void createClosureArgument(List<Expression> arguments, ClassInstanceCreation closure, List<VariableDeclarationFragment> outputs, TextEditGroup editGroup) {
//
//		for (int index= 0; index < outputs.size(); index++) {
//			VariableDeclarationFragment fragment= outputs.get(index);
//			SimpleName[] node= LinkedNodeFinder.findByNode(closure, fragment.getName());
//			for (SimpleName simpleName : node) {
//				MethodInvocation originalMethod= (MethodInvocation)ASTNodes.getParent(simpleName, MethodInvocation.class);
//				Expression target= (Expression)fRewriter.createCopyTarget((ASTNode)originalMethod.arguments().get(0));
//
//				MethodInvocation newMethod= fAST.newMethodInvocation();
//
//				MethodInvocation nestedMethod= fAST.newMethodInvocation();
//				nestedMethod.setName(fAST.newSimpleName("getOwningProcessor"));
//
//				newMethod.setExpression(nestedMethod);
//				newMethod.setName(fAST.newSimpleName("bindOutput"));
//
//				// This is safe because MethodInvocation.arguments() returns a list of Expression
//				@SuppressWarnings("unchecked")
//				List<Expression> newMethodArguments= (List<Expression>)newMethod.arguments();
//
//				NumberLiteral newNumberLiteral= fAST.newNumberLiteral();
//				newNumberLiteral.setToken(Integer.toString(index));
//				newMethodArguments.add(newNumberLiteral);
//				newMethodArguments.add(target);
//
//				fRewriter.replace(originalMethod, newMethod, editGroup);
//			}
//
//		}
//
//
//		ASTNode instanceCreationTarget= fRewriter.createMoveTarget(closure);
//		arguments.add((Expression)instanceCreationTarget);
//	}
//
//	private Expression createInputChannelExpression(List<VariableDeclarationFragment> inputs) {
//		MethodInvocation arrays= generateArraysAsListMethodInvocation();
//
//		// This is safe since MethodInvocation.arguments() returns a list of Expression
//		@SuppressWarnings("unchecked")
//		List<Expression> arguments= (List<Expression>)arrays.arguments();
//
//		if (inputs.size() == 0) {
//			// Special case
//			arguments.add(fAST.newSimpleName("channel0"));
//		} else {
//			for (VariableDeclarationFragment fragment : inputs) {
//				ASTNode name= fRewriter.createCopyTarget(fragment.getName());
//				arguments.add((Expression)name);
//			}
//		}
//
//		return arrays;
//	}
//
//
//	private Expression createChannelExpression(List<VariableDeclarationFragment> output) {
//		MethodInvocation arrays= generateArraysAsListMethodInvocation();
//
//		// This is safe since MethodInvocation.arguments() returns a list of Expression
//		@SuppressWarnings("unchecked")
//		List<Expression> arguments= (List<Expression>)arrays.arguments();
//
//		if (output.size() == 0) {
//			// Special case
//			// Leave the arguments empty so it returns an empty list
//		} else {
//			for (VariableDeclarationFragment fragment : output) {
//				ASTNode name= fRewriter.createCopyTarget(fragment.getName());
//				arguments.add((Expression)name);
//			}
//		}
//
//		return arrays;
//	}
//
//	private MethodInvocation generateArraysAsListMethodInvocation() {
//		MethodInvocation arrays= fAST.newMethodInvocation();
//		arrays.setExpression(fAST.newSimpleName("Arrays"));
//		arrays.setName(fAST.newSimpleName("asList"));
//		return arrays;
//	}
//
//	private void hoiseExistingChannels(ASTNode selectedLoopStatement, ListRewrite rewriter, TextEditGroup inverterEditGroup) {
//		for (VariableDeclarationStatement variableDeclarationStatement : fAnalyzer.getChannels()) {
//			ASTNode variableDeclarationTarget= fRewriter.createMoveTarget(variableDeclarationStatement);
//			rewriter.insertBefore(variableDeclarationTarget, selectedLoopStatement, inverterEditGroup);
//		}
//	}
//
//	//TODO: Have to handle the different direct input from the for loop and not the loop parameter
//	private void replaceForLoop(ASTNode selectedLoopStatement, ListRewrite rewriter, TextEditGroup inverterEditGroup) {
//		if (selectedLoopStatement instanceof EnhancedForStatement) {
//			EnhancedForStatement loop= (EnhancedForStatement)selectedLoopStatement;
//
//			EnhancedForStatement newLoop= fAST.newEnhancedForStatement();
//			newLoop.setParameter((SingleVariableDeclaration)fRewriter.createMoveTarget(loop.getParameter()));
//			newLoop.setExpression((Expression)fRewriter.createMoveTarget(loop.getExpression()));
//
//			// Use string generation since this is just a single statement
//			newLoop.setBody((Statement)ASTNodeFactory.newStatement(fAST, "channel0.bind(" + loop.getParameter().getName() + ");"));
//			rewriter.insertBefore(newLoop, selectedLoopStatement, inverterEditGroup);
//		}
//	}
//
//	private void addWaitForAll(ASTNode selectedLoopStatement, ListRewrite rewriter, TextEditGroup inverterEditGroup) {
//		//Use string generation since this is a single statement
//		String flowGraph= FLOWGRAPH_VARIABLE_NAME + "." + WAITFORALL_METHOD + ";";
//		ASTNode newStatement= ASTNodeFactory.newStatement(fAST, flowGraph);
//		rewriter.insertAfter(newStatement, selectedLoopStatement, inverterEditGroup);
//	}
}
