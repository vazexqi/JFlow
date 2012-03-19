package edu.illinois.jflow.core.transformations.ui.actions;

import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
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

import edu.illinois.jflow.core.transformations.code.ExtractClosureRefactoring;
import edu.illinois.jflow.core.transformations.ui.JFlowRefactoringMessages;
import edu.illinois.jflow.core.transformations.ui.refactoring.ExtractClosureWizard;

@SuppressWarnings("restriction")
public class ExtractClosureTopLevelAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	public ExtractClosureTopLevelAction() {
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window= window;
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

	private void startTextSelectionRefactoring(IEditorPart activeEditor, JavaEditor javaEditor, ISelection selection) {
		ITextSelection textSelection= (ITextSelection)selection;

		Shell shell= activeEditor.getSite().getShell();
		ExtractClosureRefactoring refactoring= new ExtractClosureRefactoring(SelectionConverter.getInputAsCompilationUnit(javaEditor), textSelection.getOffset(), textSelection.getLength());
		new RefactoringStarter().activate(new ExtractClosureWizard(refactoring), shell, JFlowRefactoringMessages.ExtractClosureAction_dialog_title, RefactoringSaveHelper.SAVE_NOTHING);
	}
}
