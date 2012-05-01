package edu.illinois.jflow.core.transformations.ui.actions;

import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Parent class for the JFlow refactoring actions.
 * 
 * @author Nicholas Chen
 * 
 */
@SuppressWarnings("restriction")
public abstract class JFlowRefactoringAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	protected abstract void startTextSelectionRefactoring(IEditorPart activeEditor, JavaEditor javaEditor, ISelection selection);

	public JFlowRefactoringAction() {
		super();
	}

	public void init(IWorkbenchWindow window) {
		this.window= window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void dispose() {
	}

	public void run(IAction action) {
		IWorkbenchPage activePage= window.getActivePage();
		IEditorPart activeEditor= activePage.getActiveEditor();

		if (activeEditor instanceof JavaEditor) {
			startJavaEditorRefactoring(activeEditor);
		}
	}

	private void startJavaEditorRefactoring(IEditorPart activeEditor) {
		JavaEditor javaEditor= (JavaEditor)activeEditor;

		if (!ActionUtil.isEditable(javaEditor))
			return;

		ISelectionService selectionService= window.getSelectionService();
		ISelection selection= selectionService.getSelection();
		if (selection instanceof ITextSelection) {
			startTextSelectionRefactoring(activeEditor, javaEditor, selection);
		}
	}

}
