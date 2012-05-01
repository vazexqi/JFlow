package edu.illinois.jflow.core.transformations.ui;

import org.eclipse.osgi.util.NLS;

public class JFlowRefactoringMessages extends NLS {
	private static final String BUNDLE_NAME= "edu.illinois.jflow.core.transformations.ui.messages"; //$NON-NLS-1$


	public static String ExtractClosureAction_dialog_title;

	public static String ExtractClosureWizard_dialog_title;

	public static String InvertLoopActions_dialog_title;

	public static String InvertLoopWizard_dialog_title;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, JFlowRefactoringMessages.class);
	}

	private JFlowRefactoringMessages() {
	}
}
