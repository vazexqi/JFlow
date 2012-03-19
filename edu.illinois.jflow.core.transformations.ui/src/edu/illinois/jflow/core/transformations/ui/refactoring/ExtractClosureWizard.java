/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package edu.illinois.jflow.core.transformations.ui.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import edu.illinois.jflow.core.transformations.code.ExtractClosureRefactoring;
import edu.illinois.jflow.core.transformations.ui.JFlowRefactoringMessages;

@SuppressWarnings("restriction")
public class ExtractClosureWizard extends RefactoringWizard {

	static final String DIALOG_SETTING_SECTION= "ExtractClosureWizard"; //$NON-NLS-1$

	public ExtractClosureWizard(ExtractClosureRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(JFlowRefactoringMessages.ExtractMethodWizard_extract_method);
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
