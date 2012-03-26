package edu.illinois.jflow.core.transformations.code;

/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [extract method] Does not replace similar code in parent class of anonymous class - https://bugs.eclipse.org/bugs/show_bug.cgi?id=160853
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [extract method] Extract method and continue https://bugs.eclipse.org/bugs/show_bug.cgi?id=48056
 *     Benjamin Muskalla <bmuskalla@eclipsesource.com> - [extract method] should declare method static if extracted from anonymous in static method - https://bugs.eclipse.org/bugs/show_bug.cgi?id=152004
 *******************************************************************************/

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

/**
 * Extracts a method in a compilation unit based on a text selection range.
 */
@SuppressWarnings("restriction")
public class ExtractClosureRefactoring extends Refactoring {

	private ICompilationUnit fCUnit;

	private CompilationUnit fRoot;

	private ImportRewrite fImportRewriter;

	private int fSelectionStart;

	private int fSelectionLength;

	private AST fAST;

	private ASTRewrite fRewriter;

	private ExtractClosureAnalyzer fAnalyzer;

	private List<ParameterInfo> fParameterInfos;

	private Set<String> fUsedNames;

	private static final String EMPTY= ""; //$NON-NLS-1$

	private static final String CLOSURE_PARAMETER_NAME= "arguments"; //$NON-NLS-1$

	private static final String CLOSURE_PARAMETER_TYPE= "Object"; //$NON-NLS-1$

	private static final String CLOSURE_METHOD= "doRun"; //$NON-NLS-1$

	private static final String CLOSURE_INVOCATION_METHOD_NAME= "call"; //$NON-NLS-1$

	private static final String CLOSURE_TYPE= "groovyx.gpars.DataflowMessagingRunnable"; //$NON-NLS-1$

	private static class UsedNamesCollector extends ASTVisitor {
		private Set<String> result= new HashSet<String>();

		private Set<SimpleName> fIgnore= new HashSet<SimpleName>();

		public static Set<String> perform(ASTNode[] nodes) {
			UsedNamesCollector collector= new UsedNamesCollector();
			for (int i= 0; i < nodes.length; i++) {
				nodes[i].accept(collector);
			}
			return collector.result;
		}

		@Override
		public boolean visit(FieldAccess node) {
			Expression exp= node.getExpression();
			if (exp != null)
				fIgnore.add(node.getName());
			return true;
		}

		@Override
		public void endVisit(FieldAccess node) {
			fIgnore.remove(node.getName());
		}

		@Override
		public boolean visit(MethodInvocation node) {
			Expression exp= node.getExpression();
			if (exp != null)
				fIgnore.add(node.getName());
			return true;
		}

		@Override
		public void endVisit(MethodInvocation node) {
			fIgnore.remove(node.getName());
		}

		@Override
		public boolean visit(QualifiedName node) {
			fIgnore.add(node.getName());
			return true;
		}

		@Override
		public void endVisit(QualifiedName node) {
			fIgnore.remove(node.getName());
		}

		@Override
		public boolean visit(SimpleName node) {
			if (!fIgnore.contains(node))
				result.add(node.getIdentifier());
			return true;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			return visitType(node);
		}

		@Override
		public boolean visit(AnnotationTypeDeclaration node) {
			return visitType(node);
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			return visitType(node);
		}

		private boolean visitType(AbstractTypeDeclaration node) {
			result.add(node.getName().getIdentifier());
			// don't dive into type declaration since they open a new
			// context.
			return false;
		}
	}

	/**
	 * Creates a new extract closure refactoring
	 * 
	 * @param unit the compilation unit, or <code>null</code> if invoked by scripting
	 * @param selectionStart selection start
	 * @param selectionLength selection end
	 */
	public ExtractClosureRefactoring(ICompilationUnit unit, int selectionStart, int selectionLength) {
		fCUnit= unit;
		fRoot= null;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
	}

	/**
	 * Creates a new extract closure refactoring
	 * 
	 * @param astRoot the AST root of an AST created from a compilation unit
	 * @param selectionStart start
	 * @param selectionLength length
	 */
	public ExtractClosureRefactoring(CompilationUnit astRoot, int selectionStart, int selectionLength) {
		this((ICompilationUnit)astRoot.getTypeRoot(), selectionStart, selectionLength);
		fRoot= astRoot;
	}

	@Override
	public String getName() {
		return JFlowRefactoringCoreMessages.ExtractClosureRefactoring_name;
	}

	/**
	 * Checks if the refactoring can be activated. Activation typically means, if a corresponding
	 * menu entry can be added to the UI.
	 * 
	 * @param pm a progress monitor to report progress during activation checking.
	 * @return the refactoring status describing the result of the activation check.
	 * @throws CoreException if checking fails
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 100); //$NON-NLS-1$

		if (fSelectionStart < 0 || fSelectionLength == 0)
			return mergeTextSelectionStatus(result);

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
		fRoot.accept(createVisitor());

		fSelectionStart= fAnalyzer.getSelection().getOffset();
		fSelectionLength= fAnalyzer.getSelection().getLength();

		result.merge(fAnalyzer.checkInitialConditions(fImportRewriter));
		if (result.hasFatalError())
			return result;
		initializeParameterInfos();
		initializeUsedNames();
		return result;
	}

	private ASTVisitor createVisitor() throws CoreException {
		fAnalyzer= new ExtractClosureAnalyzer(fCUnit, Selection.createFromStartLength(fSelectionStart, fSelectionLength));
		return fAnalyzer;
	}

	/**
	 * Returns the parameter infos.
	 * 
	 * @return a list of parameter infos.
	 */
	public List<ParameterInfo> getParameterInfos() {
		return fParameterInfos;
	}

	public ICompilationUnit getCompilationUnit() {
		return fCUnit;
	}

	/**
	 * Checks if the parameter names are valid.
	 * 
	 * @return validation status
	 */
	public RefactoringStatus checkParameterNames() {
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator<ParameterInfo> iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= iter.next();
			result.merge(Checks.checkIdentifier(parameter.getNewName(), fCUnit));
			for (Iterator<ParameterInfo> others= fParameterInfos.iterator(); others.hasNext();) {
				ParameterInfo other= others.next();
				if (parameter != other && other.getNewName().equals(parameter.getNewName())) {
					result.addError(Messages.format(
							JFlowRefactoringCoreMessages.ExtractClosureRefactoring_error_sameParameter,
							BasicElementLabels.getJavaElementName(other.getNewName())));
					return result;
				}
			}
			if (parameter.isRenamed() && fUsedNames.contains(parameter.getNewName())) {
				result.addError(Messages.format(
						JFlowRefactoringCoreMessages.ExtractClosureRefactoring_error_nameInUse,
						BasicElementLabels.getJavaElementName(parameter.getNewName())));
				return result;
			}
		}
		return result;
	}

	/**
	 * Checks if varargs are ordered correctly.
	 * 
	 * @return validation status
	 */
	public RefactoringStatus checkVarargOrder() {
		for (Iterator<ParameterInfo> iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= iter.next();
			if (info.isOldVarargs() && iter.hasNext()) {
				return RefactoringStatus.createFatalErrorStatus(Messages.format(
						JFlowRefactoringCoreMessages.ExtractClosureRefactoring_error_vararg_ordering,
						BasicElementLabels.getJavaElementName(info.getOldName())));
			}
		}
		return new RefactoringStatus();
	}

	/**
	 * Returns the names already in use in the selected statements/expressions.
	 * 
	 * @return names already in use.
	 */
	public Set<String> getUsedNames() {
		return fUsedNames;
	}

	/* (non-Javadoc)
	 * Method declared in Refactoring
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.subTask(EMPTY);

		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkParameterNames());
		result.merge(checkVarargOrder());
		pm.worked(1);
		if (pm.isCanceled())
			throw new OperationCanceledException();

		pm.done();
		return result;
	}

	/* (non-Javadoc)
	 * Method declared in IRefactoring
	 */
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		try {
			BodyDeclaration declaration= fAnalyzer.getEnclosingBodyDeclaration();
			fRewriter= ASTRewrite.create(declaration.getAST());

			final CompilationUnitChange result= new CompilationUnitChange(JFlowRefactoringCoreMessages.ExtractClosureRefactoring_change_name, fCUnit);
			result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

			MultiTextEdit root= new MultiTextEdit();
			result.setEdit(root);

			ASTNode[] selectedNodes= fAnalyzer.getSelectedNodes();

			TextEditGroup closureEditGroup= new TextEditGroup("Extract to Closure");
			result.addTextEditGroup(closureEditGroup);

			// A sentinel is just a placeholder to keep track of the position of insertion
			// For this refactoring, we need to insert two things:
			// 1) The DataflowChannels
			// 2) The DataflowMessagingRunnable
			Block sentinel= fAST.newBlock();
			ListRewrite sentinelRewriter= fRewriter.getListRewrite(selectedNodes[0].getParent(), (ChildListPropertyDescriptor)selectedNodes[0].getLocationInParent());
			sentinelRewriter.insertBefore(sentinel, selectedNodes[0], null);

			ClassInstanceCreation dataflowClosure= createNewDataflowClosure(selectedNodes, fCUnit.findRecommendedLineSeparator(), closureEditGroup);
			MethodInvocation closureInvocation= createClosureInvocation(dataflowClosure);

			sentinelRewriter.replace(sentinel, fAST.newExpressionStatement(closureInvocation), closureEditGroup);

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

	private MethodInvocation createClosureInvocation(ClassInstanceCreation dataflowClosure) {
		MethodInvocation closureInvocation= fAST.newMethodInvocation();
		closureInvocation.setName(fAST.newSimpleName(CLOSURE_INVOCATION_METHOD_NAME));
		closureInvocation.setExpression(dataflowClosure);
		createClosureArguments(closureInvocation);
		return closureInvocation;
	}

	private void createClosureArguments(MethodInvocation closureInvocation) {
		List<Expression> arguments= closureInvocation.arguments();
		for (int i= 0; i < fParameterInfos.size(); i++) {
			ParameterInfo parameter= fParameterInfos.get(i);
			arguments.add(ASTNodeFactory.newName(fAST, parameter.getOldName()));
		}
	}

	/**
	 * Create an ASTNode similar to
	 * 
	 * new DataFlowMessagingRunnable(...){...}
	 * 
	 * @param selectedNodes
	 * @param findRecommendedLineSeparator
	 * @param editGroup
	 * @return
	 */
	private ClassInstanceCreation createNewDataflowClosure(ASTNode[] selectedNodes, String findRecommendedLineSeparator, TextEditGroup editGroup) {
		ClassInstanceCreation dataflowClosure= fAST.newClassInstanceCreation();

		// Create the small chunks
		augmentWithTypeInfo(dataflowClosure);
		augmentWithConstructorArgument(dataflowClosure);
		augmentWithAnonymousClassDeclaration(dataflowClosure, selectedNodes, editGroup);

		return dataflowClosure;
	}

	private void augmentWithTypeInfo(ClassInstanceCreation dataflowClosure) {
		ImportRewriteContext context= new ContextSensitiveImportRewriteContext(fAnalyzer.getEnclosingBodyDeclaration(), fImportRewriter);
		fImportRewriter.addImport(CLOSURE_TYPE, context);
		dataflowClosure.setType(fAST.newSimpleType(fAST.newName("DataflowMessagingRunnable")));
	}

	@SuppressWarnings("unchecked")
	private void augmentWithConstructorArgument(ClassInstanceCreation dataflowClosure) {
		String argumentsCount= new Integer(fParameterInfos.size()).toString();
		dataflowClosure.arguments().add(fAST.newNumberLiteral(argumentsCount));
	}

	private void augmentWithAnonymousClassDeclaration(ClassInstanceCreation dataflowClosure, ASTNode[] selectedNodes, TextEditGroup editGroup) {
		AnonymousClassDeclaration closure= fAST.newAnonymousClassDeclaration();
		closure.bodyDeclarations().add(createRunMethodForClosure(selectedNodes, editGroup));
		dataflowClosure.setAnonymousClassDeclaration(closure);
	}

	/**
	 * Create a ASTNode similar to
	 * 
	 * protected void doRun(Object... arguments) { ... }
	 * 
	 * @param selectedNodes - The statements to be enclosed in the doRun(...) method
	 * @param editGroup
	 * @return
	 */
	private Object createRunMethodForClosure(ASTNode[] selectedNodes, TextEditGroup editGroup) {
		MethodDeclaration runMethod= fAST.newMethodDeclaration();
		runMethod.modifiers().addAll(ASTNodeFactory.newModifiers(fAST, Modifier.PROTECTED));
		runMethod.setReturnType2(fAST.newPrimitiveType(org.eclipse.jdt.core.dom.PrimitiveType.VOID));
		runMethod.setName(fAST.newSimpleName(CLOSURE_METHOD));
		runMethod.parameters().add(createObjectArrayArgument());
		runMethod.setBody(createClosureBody(selectedNodes, editGroup));
		return runMethod;
	}

	/**
	 * Creates an the Object... arguments type
	 * 
	 * @return
	 */
	private Object createObjectArrayArgument() {
		SingleVariableDeclaration parameter= fAST.newSingleVariableDeclaration();
		parameter.setVarargs(true);
		parameter.setType(fAST.newSimpleType(fAST.newSimpleName(CLOSURE_PARAMETER_TYPE)));
		parameter.setName(fAST.newSimpleName(CLOSURE_PARAMETER_NAME));
		return parameter;
	}

	private Block createClosureBody(ASTNode[] selectedNodes, TextEditGroup editGroup) {
		Block methodBlock= fAST.newBlock();
		ListRewrite statements= fRewriter.getListRewrite(methodBlock, Block.STATEMENTS_PROPERTY);

		// Locals that are not passed as an arguments since the extracted method only
		// writes to them
		IVariableBinding[] methodLocals= fAnalyzer.getMethodLocals();
		for (int i= 0; i < methodLocals.length; i++) {
			if (methodLocals[i] != null) {
				methodBlock.statements().add(createDeclaration(methodLocals[i], null));
			}
		}

		int argumentPosition= 0;
		for (ParameterInfo parameter : fParameterInfos) {
			for (int n= 0; n < selectedNodes.length; n++) {
				SimpleName[] oldNames= LinkedNodeFinder.findByBinding(selectedNodes[n], parameter.getOldBinding());
				for (int i= 0; i < oldNames.length; i++) {
					fRewriter.replace(oldNames[i], createCastParameters(parameter, argumentPosition), null);
				}
			}
			argumentPosition++;
		}

		ListRewrite source= fRewriter.getListRewrite(
				selectedNodes[0].getParent(),
				(ChildListPropertyDescriptor)selectedNodes[0].getLocationInParent());
		ASTNode toMove= source.createMoveTarget(
				selectedNodes[0], selectedNodes[selectedNodes.length - 1], null, editGroup);
		statements.insertLast(toMove, editGroup);
		return methodBlock;
	}

	private ASTNode createCastParameters(ParameterInfo parameter, int argumentsPosition) {
		ParenthesizedExpression argumentExpression= fAST.newParenthesizedExpression();
		CastExpression castExpression= fAST.newCastExpression();

		VariableDeclaration infoDecl= getVariableDeclaration(parameter);
		castExpression.setType(ASTNodeFactory.newType(fAST, infoDecl, fImportRewriter, null));

		ArrayAccess arrayAccess= fAST.newArrayAccess();
		arrayAccess.setArray(fAST.newSimpleName(CLOSURE_PARAMETER_NAME));
		arrayAccess.setIndex(fAST.newNumberLiteral(Integer.toString(argumentsPosition)));
		castExpression.setExpression(arrayAccess);

		argumentExpression.setExpression(castExpression);
		return argumentExpression;
	}



	//---- Helper methods ------------------------------------------------------------------------

	private void initializeParameterInfos() {
		IVariableBinding[] arguments= fAnalyzer.getArguments();
		fParameterInfos= new ArrayList<ParameterInfo>(arguments.length);
		ASTNode root= fAnalyzer.getEnclosingBodyDeclaration();
		ParameterInfo vararg= null;
		for (int i= 0; i < arguments.length; i++) {
			IVariableBinding argument= arguments[i];
			if (argument == null)
				continue;
			VariableDeclaration declaration= ASTNodes.findVariableDeclaration(argument, root);
			boolean isVarargs= declaration instanceof SingleVariableDeclaration
					? ((SingleVariableDeclaration)declaration).isVarargs()
					: false;
			ParameterInfo info= new ParameterInfo(argument, getType(declaration, isVarargs), argument.getName(), i);
			if (isVarargs) {
				vararg= info;
			} else {
				fParameterInfos.add(info);
			}
		}
		if (vararg != null) {
			fParameterInfos.add(vararg);
		}
	}

	private void initializeUsedNames() {
		fUsedNames= UsedNamesCollector.perform(fAnalyzer.getSelectedNodes());
		for (Iterator<ParameterInfo> iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo parameter= iter.next();
			fUsedNames.remove(parameter.getOldName());
		}
	}

	private RefactoringStatus mergeTextSelectionStatus(RefactoringStatus status) {
		status.addFatalError(JFlowRefactoringCoreMessages.ExtractClosureRefactoring_no_set_of_statements);
		return status;
	}

	private String getType(VariableDeclaration declaration, boolean isVarargs) {
		String type= ASTNodes.asString(ASTNodeFactory.newType(declaration.getAST(), declaration, fImportRewriter, new ContextSensitiveImportRewriteContext(declaration, fImportRewriter)));
		if (isVarargs)
			return type + ParameterInfo.ELLIPSIS;
		else
			return type;
	}

	//---- Code generation -----------------------------------------------------------------------

	private VariableDeclaration getVariableDeclaration(ParameterInfo parameter) {
		return ASTNodes.findVariableDeclaration(parameter.getOldBinding(), fAnalyzer.getEnclosingBodyDeclaration());
	}

	private VariableDeclarationStatement createDeclaration(IVariableBinding binding, Expression intilizer) {
		VariableDeclaration original= ASTNodes.findVariableDeclaration(binding, fAnalyzer.getEnclosingBodyDeclaration());
		VariableDeclarationFragment fragment= fAST.newVariableDeclarationFragment();
		fragment.setName((SimpleName)ASTNode.copySubtree(fAST, original.getName()));
		fragment.setInitializer(intilizer);
		VariableDeclarationStatement result= fAST.newVariableDeclarationStatement(fragment);
		result.modifiers().addAll(ASTNode.copySubtrees(fAST, ASTNodes.getModifiers(original)));
		result.setType(ASTNodeFactory.newType(fAST, original, fImportRewriter, new ContextSensitiveImportRewriteContext(original, fImportRewriter)));
		return result;
	}
}
