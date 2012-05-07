package edu.illinois.jflow.core.transformations.ui.refactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.illinois.jflow.core.transformations.code.InvertLoopRefactoring;
import edu.illinois.jflow.core.transformations.ui.JFlowRefactoringMessages;

/**
 * Invert Loop Refactoring wizard
 * 
 * @author Nicholas Chen
 * 
 */
@SuppressWarnings("restriction")
public class InvertLoopWizard extends RefactoringWizard {

	public InvertLoopWizard(InvertLoopRefactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(JFlowRefactoringMessages.InvertLoopWizard_dialog_title);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
	}

	@Override
	protected void addUserInputPages() {
		addPage(new InvertLoopInputPage());
	}

}
