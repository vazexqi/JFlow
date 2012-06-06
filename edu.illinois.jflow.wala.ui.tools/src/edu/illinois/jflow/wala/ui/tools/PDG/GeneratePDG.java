package edu.illinois.jflow.wala.ui.tools.PDG;

import java.io.IOException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.util.CancelException;

import edu.illinois.jflow.wala.utils.EclipseProjectAnalysisEngine;

@SuppressWarnings("restriction")
public class GeneratePDG implements IViewActionDelegate {

	@Override
	public void run(IAction action) {
		IEditorPart activeEditor= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (activeEditor instanceof JavaEditor) {
			JavaEditor javaEditor= (JavaEditor)activeEditor;
			ICompilationUnit inputAsCompilationUnit= SelectionConverter.getInputAsCompilationUnit(javaEditor);
			IJavaProject javaProject= inputAsCompilationUnit.getJavaProject();
			AbstractAnalysisEngine engine= new EclipseProjectAnalysisEngine(javaProject);
			try {
				engine.buildDefaultCallGraph();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (CancelException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

	@Override
	public void init(IViewPart view) {
	}

}
