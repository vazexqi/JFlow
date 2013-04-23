/**
 * This class derives
 * from {@link org.eclipse.jdt.internal.ui.refactoring.code.ExtractMethodInputPage} and is
 * licensed under the Eclipse Public License.
 */
package edu.illinois.jflow.core.transformations.ui.refactoring;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import edu.illinois.jflow.core.transformations.code.ExtractClosureRefactoring;

/**
 * This is the input page for the Extract Closure Refactoring.
 * 
 * @author nchen
 * 
 */
public class ExtractClosureInputPage extends UserInputWizardPage {

	public static final String PAGE_NAME= "ExtractClosureInputPage";//$NON-NLS-1$

	private ExtractClosureRefactoring fRefactoring;

	private IDialogSettings fSettings;

	public ExtractClosureInputPage() {
		super(PAGE_NAME);
		setDescription(JFlowRefactoringUIMessages.ExtractClosureInputPage_description);
	}

	public void createControl(Composite parent) {
		fRefactoring= (ExtractClosureRefactoring)getRefactoring();
		loadSettings();

		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);
		GridData gd= null;

		initializeDialogUnits(result);

		Composite group= new Composite(result, SWT.NONE);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layout= new GridLayout();
		layout.numColumns= 4;
		layout.marginWidth= 0;
		group.setLayout(layout);

// XXX: Add some parameters if we really need them
//		if (!fRefactoring.getParameterInfos().isEmpty()) {
//			ChangeParametersControl cp= new ChangeParametersControl(result, SWT.NONE,
//					JFlowRefactoringUIMessages.ExtractClosureInputPage_parameters,
//					new IParameterListChangeListener() {
//						public void parameterChanged(ParameterInfo parameter) {
//							parameterModified();
//						}
//
//						public void parameterListChanged() {
//							parameterModified();
//						}
//
//						public void parameterAdded(ParameterInfo parameter) {
//						}
//					}, ChangeParametersControl.Mode.EXTRACT_METHOD);
//			gd= new GridData(GridData.FILL_BOTH);
//			gd.horizontalSpan= 2;
//			cp.setLayoutData(gd);
//			cp.setInput(fRefactoring.getParameterInfos());
//		}
//
//		Dialog.applyDialogFont(result);
	}


	private void loadSettings() {
		fSettings= getDialogSettings().getSection(ExtractClosureWizard.DIALOG_SETTING_SECTION);
		if (fSettings == null) {
			fSettings= getDialogSettings().addNewSection(ExtractClosureWizard.DIALOG_SETTING_SECTION);
		}
	}

	//---- Input validation ------------------------------------------------------

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
//			setPageComplete(validatePage(true));
			setPageComplete(true);
		}
		super.setVisible(visible);
	}
}
