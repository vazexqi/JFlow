/**
 * This class derives
 * from {@link org.eclipse.jdt.internal.ui.refactoring.code.ExtractMethodInputPage} and is
 * licensed under the Eclipse Public License.
 */
package edu.illinois.jflow.core.transformations.ui.refactoring;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.ui.refactoring.ChangeParametersControl;
import org.eclipse.jdt.internal.ui.refactoring.IParameterListChangeListener;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import edu.illinois.jflow.core.transformations.code.ExtractClosureRefactoring;

/**
 * This is the input page for the Extract Closure Refactoring.
 * 
 * @author Nicholas Chen
 * 
 */
@SuppressWarnings("restriction")
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

		//TODO: Should not have an empty parameter (need to substitute with continue node)
		if (!fRefactoring.getParameterInfos().isEmpty()) {
			ChangeParametersControl cp= new ChangeParametersControl(result, SWT.NONE,
					JFlowRefactoringUIMessages.ExtractClosureInputPage_parameters,
					new IParameterListChangeListener() {
						public void parameterChanged(ParameterInfo parameter) {
							parameterModified();
						}

						public void parameterListChanged() {
							parameterModified();
						}

						public void parameterAdded(ParameterInfo parameter) {
						}
					}, ChangeParametersControl.Mode.EXTRACT_METHOD);
			gd= new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan= 2;
			cp.setLayoutData(gd);
			cp.setInput(fRefactoring.getParameterInfos());
		}

		Dialog.applyDialogFont(result);
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
			setPageComplete(validatePage(true));
		}
		super.setVisible(visible);
	}


	private void parameterModified() {
		setPageComplete(validatePage(false));
	}

	private RefactoringStatus validatePage(boolean text) {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(validateParameters());
		return result;
	}


	private RefactoringStatus validateParameters() {
		RefactoringStatus result= new RefactoringStatus();
		List<ParameterInfo> parameters= fRefactoring.getParameterInfos();
		for (Iterator<ParameterInfo> iter= parameters.iterator(); iter.hasNext();) {
			ParameterInfo info= iter.next();
			if ("".equals(info.getNewName())) { //$NON-NLS-1$
				result.addFatalError(JFlowRefactoringUIMessages.ExtractClosureInputPage_validation_emptyParameterName);
				return result;
			}
		}
		result.merge(fRefactoring.checkVarargOrder());
		return result;
	}

}
