package edu.illinois.jflow.core.transformations.ui.actions;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Shell;

import edu.illinois.jflow.core.transformations.code.ExtractClosureRefactoring;
import edu.illinois.jflow.core.transformations.ui.JFlowRefactoringMessages;
import edu.illinois.jflow.core.transformations.ui.refactoring.ExtractClosureWizard;

/**
 * Context menu action to invoke our Extract Closure Refactoring
 * 
 * @author Nicholas Chen
 * 
 */
@SuppressWarnings("restriction")
public class ExtractClosureContextAction extends JFlowContextAction {

	@Override
	protected void startTextSelectionRefactoring(JavaEditor javaEditor, ITextSelection textSelection) {
		Shell shell= javaEditor.getSite().getShell();
		IDocument doc= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
		ExtractClosureRefactoring refactoring= new ExtractClosureRefactoring(SelectionConverter.getInputAsCompilationUnit(javaEditor), doc, textSelection.getOffset(), textSelection.getLength());
		new RefactoringStarter().activate(new ExtractClosureWizard(refactoring), shell, JFlowRefactoringMessages.ExtractClosureAction_dialog_title, RefactoringSaveHelper.SAVE_NOTHING);
	}
}
