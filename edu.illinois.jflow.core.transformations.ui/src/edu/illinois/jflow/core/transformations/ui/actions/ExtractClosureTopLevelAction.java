package edu.illinois.jflow.core.transformations.ui.actions;

import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.code.ExtractMethodWizard;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

@SuppressWarnings("restriction")
public class ExtractClosureTopLevelAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	public ExtractClosureTopLevelAction() {
	}

	public void run(IAction action) {
		IWorkbenchPage activePage= window.getActivePage();
		IEditorPart activeEditor= activePage.getActiveEditor();

		if (activeEditor instanceof JavaEditor) {
			JavaEditor javaEditor= (JavaEditor)activeEditor;

			if (!ActionUtil.isEditable(javaEditor))
				return;

			ISelectionService selectionService= window.getSelectionService();
			ISelection selection= selectionService.getSelection();
			if (selection instanceof ITextSelection) {
				ITextSelection textSelection= (ITextSelection)selection;

				Shell shell= activeEditor.getSite().getShell();
				ExtractMethodRefactoring refactoring= new ExtractMethodRefactoring(SelectionConverter.getInputAsCompilationUnit(javaEditor), textSelection.getOffset(), textSelection.getLength());
				new RefactoringStarter().activate(new ExtractMethodWizard(refactoring), shell, RefactoringMessages.ExtractMethodAction_dialog_title, RefactoringSaveHelper.SAVE_NOTHING);
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window= window;
	}
}
