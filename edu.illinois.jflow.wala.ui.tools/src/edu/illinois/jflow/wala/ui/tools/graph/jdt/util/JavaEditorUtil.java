package edu.illinois.jflow.wala.ui.tools.graph.jdt.util;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.translator.jdt.JDTIdentityMapper;
import com.ibm.wala.types.MethodReference;

@SuppressWarnings("restriction")
public class JavaEditorUtil {
	public static JavaEditor getActiveJavaEditor() {
		IEditorPart activeEditor= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (activeEditor instanceof JavaEditor) {
			return (JavaEditor)activeEditor;
		}
		return null;
	}

	public static MethodReference findSelectedMethodDeclaration(JavaEditor javaEditor, ICompilationUnit inputAsCompilationUnit) {
		ITextSelection selection= (ITextSelection)javaEditor.getSelectionProvider().getSelection();

		// 1) Get the ast for the editor
		CompilationUnit ast= SharedASTProvider.getAST(inputAsCompilationUnit, SharedASTProvider.WAIT_ACTIVE_ONLY, new NullProgressMonitor());

		// 2) Find the selected node
		NodeFinder nodeFinder= new NodeFinder(ast, selection.getOffset(), selection.getLength());
		ASTNode coveringNode= nodeFinder.getCoveringNode();

		// 3) From the selected node, get the closest MethodDeclaration
		MethodDeclaration methodDeclaration= (MethodDeclaration)ASTNodes.getParent(coveringNode, MethodDeclaration.class);
		JDTIdentityMapper mapper= new JDTIdentityMapper(JavaSourceAnalysisScope.SOURCE, ast.getAST());
		return mapper.getMethodRef(methodDeclaration.resolveBinding());

	}
}
