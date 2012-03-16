package edu.illinois.jflow.core.transformations.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class ExtractClosureTopLevelAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	public ExtractClosureTopLevelAction() {
	}

	public void run(IAction action) {
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window= window;
	}
}
