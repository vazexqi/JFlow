/**
 * This class derives
 * from {@link org.eclipse.jdt.internal.ui.refactoring.code.ExtractMethodWizard} and is
 * licensed under the Eclipse Public License.
 */
package edu.illinois.jflow.core.transformations.ui.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.illinois.jflow.core.transformations.code.ExtractClosureRefactoring;
import edu.illinois.jflow.core.transformations.ui.JFlowRefactoringMessages;

/**
 * Extract Closure Refactoring wizard.
 * 
 * @author Nicholas Chen
 * 
 */
@SuppressWarnings("restriction")
public class ExtractClosureWizard extends RefactoringWizard {

	static final String DIALOG_SETTING_SECTION= "ExtractClosureWizard"; //$NON-NLS-1$

	public ExtractClosureWizard(ExtractClosureRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(JFlowRefactoringMessages.ExtractClosureWizard_extract_method);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
	}

	public Change createChange() {
		// creating the change is cheap. So we don't need to show progress.
		try {
			return getRefactoring().createChange(new NullProgressMonitor());
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return null;
		}
	}

	@Override
	protected void addUserInputPages() {
		addPage(new ExtractClosureInputPage());
	}
}
