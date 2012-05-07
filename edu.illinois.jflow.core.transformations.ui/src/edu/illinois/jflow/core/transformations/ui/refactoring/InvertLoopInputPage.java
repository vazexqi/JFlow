package edu.illinois.jflow.core.transformations.ui.refactoring;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import edu.illinois.jflow.core.transformations.code.InvertLoopRefactoring;

public class InvertLoopInputPage extends UserInputWizardPage {
	public static final String PAGE_NAME= "InvertLoopInputPage";//$NON-NLS-1$

	private InvertLoopRefactoring fRefactoring;

	public InvertLoopInputPage() {
		super(PAGE_NAME);
		setDescription(JFlowRefactoringUIMessages.ExtractClosureInputPage_description);
	}

	@Override
	public void createControl(Composite parent) {
		fRefactoring= (InvertLoopRefactoring)getRefactoring();
		Composite result= new Composite(parent, SWT.NONE);
		setControl(result);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		result.setLayout(layout);

		initializeDialogUnits(result);
	}
}
