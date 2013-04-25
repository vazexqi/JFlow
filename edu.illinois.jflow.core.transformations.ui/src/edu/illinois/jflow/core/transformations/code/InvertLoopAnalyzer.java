package edu.illinois.jflow.core.transformations.code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

@SuppressWarnings("restriction")
class InvertLoopAnalyzer extends SelectionAnalyzer {

	private final class DataflowVisitor extends GenericVisitor {

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visit(org.eclipse.jdt.core.dom.ClassInstanceCreation)
		 * 
		 * We visit all ClassInstanceCreation sites to see if we an instantiation of our DataflowMessagingRunnable (or any of its subtypes).
		 * We then use this is a starting point to traverse the tree to obtain the whole ExpressionStatement.
		 * 
		 * An ExpressionStatement is a statement of the form:
		 * 
		 * new DataflowMessagingRunnable(1) {
		 *  @Override
		 *  protected void doRun(Object... args) {
		 *  ...
		 *  }
		 * }.call(channel0.getVal());
		 * 
		 * Note we want the ExpressionStatement so that the trailing semicolon is also captured.
		 */
		@Override
		public boolean visit(ClassInstanceCreation node) {
			Type type= node.getType();
			ITypeBinding binding= type.resolveBinding();
			IType javaElement= (IType)binding.getJavaElement();

			try {
				ITypeHierarchy superTypeHierarchy= javaElement.newSupertypeHierarchy(fPM);
				IType[] allClasses= superTypeHierarchy.getAllClasses();
				for (IType classType : allClasses) {
					String fullyQualifiedName= classType.getFullyQualifiedName();
					if (fullyQualifiedName.equals(ExtractClosureRefactoring.DATAFLOWMESSAGING_TYPE)) {
						// Grab the top level ExpressionStatement
						ExpressionStatement expressionStatement= (ExpressionStatement)ASTNodes.getParent(node, ExpressionStatement.class);
						fClosures.add(expressionStatement);
					}
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}

			return super.visit(node);
		}
	}

	private RefactoringStatus fStatus;

	private IProgressMonitor fPM;

	private List<ExpressionStatement> fClosures;

	private CompilationUnit fRoot;

	public InvertLoopAnalyzer(Selection selection, CompilationUnit fRoot, IProgressMonitor pm) throws CoreException {
		super(selection, false);

		this.fRoot= fRoot;
		this.fPM= pm;

		fStatus= new RefactoringStatus();
		fClosures= new ArrayList<ExpressionStatement>();
	}


	public RefactoringStatus checkInitialConditions(ImportRewrite fImportRewriter) {
		RefactoringStatus status= fStatus;

		checkLoopSelected(status);
		if (status.hasFatalError())
			return status;

		checkContainsDataflowClosure(status);
		return status;
	}

	private void checkLoopSelected(RefactoringStatus status) {
		ASTNode lastCoveringNode= locateEnclosingLoopStatement();
		if (!(lastCoveringNode instanceof EnhancedForStatement || lastCoveringNode instanceof ForStatement)) {
			status.addFatalError(JFlowRefactoringCoreMessages.InvertLoopAnalyzer_not_on_loop);
		}
	}

	private void checkContainsDataflowClosure(RefactoringStatus status) {
		DataflowVisitor visitor= new DataflowVisitor();
		locateEnclosingLoopStatement().accept(visitor);
		if (fClosures.size() <= 0) {
			status.addFatalError(JFlowRefactoringCoreMessages.InvertLoopAnalyzer_does_not_contain_closure);
		}

		// DEBUGGING
		for (ExpressionStatement expr : fClosures) {
			System.out.println(ASTNodes.asString(expr));
		}
	}

	private ASTNode locateEnclosingLoopStatement() {
		Selection selection= getSelection();
		NodeFinder nodeFinder= new NodeFinder(fRoot, selection.getOffset(), selection.getLength());
		ASTNode node= nodeFinder.getCoveringNode();
		do {
			node= node.getParent();
		} while (node != null && !(node instanceof EnhancedForStatement || node instanceof ForStatement));
		return node;
	}

	public BodyDeclaration getEnclosingBodyDeclaration() {
		return (BodyDeclaration)ASTNodes.getParent(locateEnclosingLoopStatement(), BodyDeclaration.class);
	}

	/*
	 * Because we are visiting the DataflowMessagingRunnables in order, we can be certain that we are listing them by stage ordering. That means the first one is stage1, the second is stage2, etc.
	 */
	public List<ExpressionStatement> getfClosures() {
		return fClosures;
	}
}
