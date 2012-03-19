package edu.illinois.jflow.core.transformations.ui;

import org.eclipse.osgi.util.NLS;

public class JFlowRefactoringMessages extends NLS {
	private static final String BUNDLE_NAME= "edu.illinois.jflow.core.transformations.ui.messages"; //$NON-NLS-1$

	public static String ExtractClosureAction_dialog_title;

	public static String ExtractMethodWizard_extract_method;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, JFlowRefactoringMessages.class);
	}

	private JFlowRefactoringMessages() {
	}
}
