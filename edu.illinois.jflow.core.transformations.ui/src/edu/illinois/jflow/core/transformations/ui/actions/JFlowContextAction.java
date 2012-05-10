package edu.illinois.jflow.core.transformations.ui.actions;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

/**
 * Parent class for the JFlow refactoring context menu actions
 * 
 * @author Nicholas Chen
 * 
 */
@SuppressWarnings("restriction")
public abstract class JFlowContextAction implements IEditorActionDelegate {

	protected IEditorPart fEditor;

	protected ISelection fSelection;

	abstract protected void startTextSelectionRefactoring(JavaEditor javaEditor, ITextSelection textSelection);

	@Override
	public void run(IAction action) {
		if (fEditor instanceof JavaEditor) {
			if (fSelection instanceof ITextSelection) {
				startTextSelectionRefactoring((JavaEditor)fEditor, (ITextSelection)fSelection);
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
	}

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		fEditor= targetEditor;
	}

}
