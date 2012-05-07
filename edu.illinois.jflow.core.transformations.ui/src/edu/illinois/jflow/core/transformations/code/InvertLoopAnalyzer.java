package edu.illinois.jflow.core.transformations.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

@SuppressWarnings("restriction")
class InvertLoopAnalyzer extends SelectionAnalyzer {

	// TODO: For now just checks that we have a closure in the selection
	private final class DataflowVisitor extends GenericVisitor {

		// There is some duplication here since we cannot easily extract a method that can handle both ClassInstanceCreation and 
		// VariableDeclarationStatement. Their common supertype, i.e., Statement does not support a getType() method.
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
					if (fullyQualifiedName.equals(ExtractClosureRefactoring.CLOSURE_TYPE)) {
						getClosures().add(node);
					}
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			}

			return super.visit(node);
		}

		@Override
		public void endVisit(VariableDeclarationStatement node) {
			Type type= node.getType();
			if (!type.isPrimitiveType()) {
				ITypeBinding binding= type.resolveBinding();
				IType javaElement= (IType)binding.getJavaElement();


				try {
					ITypeHierarchy superTypeHierarchy= javaElement.newSupertypeHierarchy(fPM);
					IType[] allInterfaces= superTypeHierarchy.getAllInterfaces();
					for (IType interfaceType : allInterfaces) {
						String fullyQualifiedName= interfaceType.getFullyQualifiedName();
						if (fullyQualifiedName.equals(ExtractClosureRefactoring.DATAFLOWQUEUE_INTERFACE)) {
							getChannels().add(node);
						}
					}
				} catch (JavaModelException e) {
					e.printStackTrace();
				}
			}

			super.endVisit(node);
		}
	}

	private RefactoringStatus fStatus;

	private IProgressMonitor fPM;

	private List<ClassInstanceCreation> closures;

	private List<VariableDeclarationStatement> channels;

	public static class InOutChannelInformation {
		List<VariableDeclarationFragment> inputs;

		List<VariableDeclarationFragment> outputs;

		public InOutChannelInformation() {
			inputs= new ArrayList<VariableDeclarationFragment>();
			outputs= new ArrayList<VariableDeclarationFragment>();
		}

		@Override
		public String toString() {
			StringBuilder builder= new StringBuilder();

			builder.append("Inputs:\n");
			builder.append(inputs);
			builder.append("\n");

			builder.append("Outputs:\n");
			builder.append(outputs);
			builder.append("\n");

			return builder.toString();
		}
	}

	private Map<ClassInstanceCreation, InOutChannelInformation> closures2channels;

	public InvertLoopAnalyzer(Selection selection, IProgressMonitor pm) throws CoreException {
		super(selection, false);
		fPM= pm;
		fStatus= new RefactoringStatus();
		closures= new ArrayList<ClassInstanceCreation>();
		channels= new ArrayList<VariableDeclarationStatement>();
		closures2channels= new HashMap<ClassInstanceCreation, InvertLoopAnalyzer.InOutChannelInformation>();
	}

	public RefactoringStatus getStatus() {
		return fStatus;
	}

	public RefactoringStatus checkInitialConditions(ImportRewrite fImportRewriter) {
		RefactoringStatus status= getStatus();

		checkLoopSelected(status);
		if (status.hasFatalError())
			return status;

		checkContainsDataflowClosure(status);

		determineChannelReferences(fPM, status);

		return status;
	}

	private void determineChannelReferences(IProgressMonitor monitor, RefactoringStatus status) {
		for (VariableDeclarationStatement channelDeclarationStatement : channels) {
			// This is safe because VariableDeclarationStatement.fragment returns a list of VariableDeclarationFragment
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> fragments= (List<VariableDeclarationFragment>)channelDeclarationStatement.fragments();
			for (VariableDeclarationFragment fragment : fragments) {
				determineInOutChannelInformation(fragment);

			}

		}
	}

	private void determineInOutChannelInformation(VariableDeclarationFragment fragment) {
		for (ClassInstanceCreation closure : closures) {
			InOutChannelInformation information= closures2channels.get(closure);
			if (information == null) {
				information= new InOutChannelInformation();
				closures2channels.put(closure, information);
			}

			determineOutChannels(closure, information, fragment);
			determineInputChannels(closure, information, fragment);

		}

	}


	private void determineOutChannels(ClassInstanceCreation closure, InOutChannelInformation information, VariableDeclarationFragment fragment) {
		IBinding channelBinding= fragment.getName().resolveBinding();
		SimpleName[] findByBinding= LinkedNodeFinder.findByBinding(closure, channelBinding);
		if (findByBinding.length != 0)
			information.outputs.add(fragment);
	}

	private void determineInputChannels(ClassInstanceCreation closure, InOutChannelInformation information, VariableDeclarationFragment fragment) {
		IBinding channelBinding= fragment.getName().resolveBinding();
		MethodInvocation methodInvocation= (MethodInvocation)ASTNodes.getParent(closure, MethodInvocation.class);
		if (methodInvocation != null) {
			// This is safe because MethodInvocatoin.arguments returns a list of Expression
			@SuppressWarnings("unchecked")
			List<Expression> arguments= (List<Expression>)methodInvocation.arguments();

			//TODO: This is a very weak check
			for (Expression expression : arguments) {
				if (expression instanceof SimpleName) {
					SimpleName name= (SimpleName)expression;
					IBinding binding= name.resolveBinding();
					ASTNode declaringNode= getCompilationUnit().findDeclaringNode(binding);
					SimpleName[] findByBinding= LinkedNodeFinder.findByBinding(declaringNode, channelBinding);
					if (findByBinding.length != 0)
						information.inputs.add(fragment);
				}
			}
		}

	}

	//TODO: Make this work on different types of loops
	private void checkLoopSelected(RefactoringStatus status) {
		ASTNode lastCoveringNode= getLastCoveringNode();
		if (!(lastCoveringNode instanceof EnhancedForStatement)) {
			status.addFatalError(JFlowRefactoringCoreMessages.InvertLoopAnalyzer_not_on_loop);
		}
	}

	//TODO: Make this check more robust
	private void checkContainsDataflowClosure(RefactoringStatus status) {
		DataflowVisitor visitor= new DataflowVisitor();
		getSelectedLoopStatement().accept(visitor);
		if (closures.size() <= 0 || channels.size() <= 0) {
			status.addFatalError(JFlowRefactoringCoreMessages.InvertLoopAnalyzer_does_not_contain_closure);
		}
	}

	private CompilationUnit getCompilationUnit() {
		return (CompilationUnit)getEnclosingBodyDeclaration().getRoot();
	}

	public ASTNode getSelectedLoopStatement() {
		return getLastCoveringNode();
	}

	public BodyDeclaration getEnclosingBodyDeclaration() {
		return (BodyDeclaration)ASTNodes.getParent(getSelectedLoopStatement(), BodyDeclaration.class);
	}

	public List<ClassInstanceCreation> getClosures() {
		return closures;
	}

	public List<VariableDeclarationStatement> getChannels() {
		return channels;
	}

	public Map<ClassInstanceCreation, InOutChannelInformation> getClosures2channels() {
		return closures2channels;
	}

}
