package edu.illinois.jflow.core.transformations.ui.actions;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Shell;

import edu.illinois.jflow.core.transformations.code.InvertLoopRefactoring;
import edu.illinois.jflow.core.transformations.ui.JFlowRefactoringMessages;
import edu.illinois.jflow.core.transformations.ui.refactoring.InvertLoopWizard;

/**
 * Action to invoke our Invert Loop Refactoring
 * 
 * @author Nicholas Chen
 * 
 */
@SuppressWarnings("restriction")
public class InvertLoopTopLevelAction extends JFlowRefactoringAction {

	@Override
	protected void startTextSelectionRefactoring(JavaEditor javaEditor, ITextSelection textSelection) {
		Shell shell= javaEditor.getSite().getShell();
		InvertLoopRefactoring refactoring= new InvertLoopRefactoring(SelectionConverter.getInputAsCompilationUnit(javaEditor), textSelection.getOffset(), textSelection.getLength());
		new RefactoringStarter().activate(new InvertLoopWizard(refactoring), shell, JFlowRefactoringMessages.InvertLoopActions_dialog_title, RefactoringSaveHelper.SAVE_NOTHING);
	}
}
