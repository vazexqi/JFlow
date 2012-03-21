package edu.illinois.jflow.core.transformations.ui.refactoring;

import org.eclipse.osgi.util.NLS;

public class JFlowRefactoringUIMessages extends NLS {
	private static final String BUNDLE_NAME= "edu.illinois.jflow.core.transformations.ui.refactoring.refactoringui"; //$NON-NLS-1$

	public static String ExtractClosureInputPage_description;

	public static String ExtractClosureInputPage_parameters;

	public static String ExtractClosureInputPage_validation_emptyParameterName;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, JFlowRefactoringUIMessages.class);
	}

	private JFlowRefactoringUIMessages() {
	}
}
