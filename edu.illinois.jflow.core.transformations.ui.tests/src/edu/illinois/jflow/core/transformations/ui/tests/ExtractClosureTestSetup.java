package edu.illinois.jflow.core.transformations.ui.tests;

import junit.framework.Test;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTestSetup;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

@SuppressWarnings("restriction")
public class ExtractClosureTestSetup extends RefactoringTestSetup {

	private IPackageFragment basicPackage;

	public ExtractClosureTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();

		// Need to correct the default JRE because WALA needs a "real" one not some fake one
		// Remove the fake rt.jar

		// Add the real library

		RefactoringCore.getUndoManager().flush();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		basicPackage= root.createPackageFragment("basic_in", true, null);
	}

	public IPackageFragment getBasicPackage() {
		return basicPackage;
	}
}
