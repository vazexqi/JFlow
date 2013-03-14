package edu.illinois.jflow.ui.tools.pdg;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;

import com.ibm.wala.shrikeCT.InvalidClassFileException;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;
import edu.illinois.jflow.wala.ui.tools.graph.jdt.util.JavaEditorUtil;

@SuppressWarnings("restriction")
public class ViewPDGAction extends Action {

	private final PDGView view;

	public ViewPDGAction(PDGView view) {
		this.view= view;
	}

	@Override
	public void run() {
		JavaEditor javaEditor= JavaEditorUtil.getActiveJavaEditor();
		if (javaEditor != null) {
			ICompilationUnit inputAsCompilationUnit= SelectionConverter.getInputAsCompilationUnit(javaEditor);
			IJavaProject javaProject= inputAsCompilationUnit.getJavaProject();
			try {
				ProgramDependenceGraph graph= PDGGenerator.makePDGForSelectedMethodInEditor(javaEditor, inputAsCompilationUnit, javaProject);
				IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
				view.setDocument(document);
				view.setPDG(graph);
				view.updateGraph(graph);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (InvalidClassFileException e) {
				e.printStackTrace();
			}

		}

	}


}
