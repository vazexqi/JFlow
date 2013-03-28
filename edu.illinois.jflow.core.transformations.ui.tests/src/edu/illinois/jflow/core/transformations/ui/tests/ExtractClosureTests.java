package edu.illinois.jflow.core.transformations.ui.tests;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ui.tests.refactoring.AbstractSelectionTestCase;
import org.eclipse.jdt.ui.tests.refactoring.TestModelProvider;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.illinois.jflow.core.transformations.code.ExtractClosureRefactoring;

@SuppressWarnings("restriction")
public class ExtractClosureTests extends AbstractSelectionTestCase {
	private static ExtractClosureTestSetup fgTestSetup;

	public ExtractClosureTests(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new ExtractClosureTestSetup(new TestSuite(ExtractClosureTests.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test test) {
		fgTestSetup= new ExtractClosureTestSetup(test);
		return fgTestSetup;
	}

	protected InputStream getFileInputStream(String fileName) throws IOException {
		return Activator.getDefault().getTestResourceStream(fileName);
	}

	protected void setUp() throws Exception {
		super.setUp();
		fIsPreDeltaTest= true;
	}

	protected String getResourceLocation() {
		return "ExtractClosureTests/";
	}

	protected String adaptName(String name) {
		return name + "_" + getName() + ".java";
	}

	protected void performTest(IPackageFragment packageFragment, String id, String outputFolder) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, id);
		int[] selection= getSelection();
		ExtractClosureRefactoring refactoring= new ExtractClosureRefactoring(unit, new Document(unit.getSource()), selection[0], selection[1]);
		TestModelProvider.clearDelta();
		RefactoringStatus status= refactoring.checkInitialConditions(new NullProgressMonitor());

		// Must have valid preconditions
		assertTrue(status.isOK());

		String out= getProofedContent(outputFolder, id);
		performTest(unit, refactoring, COMPARE_WITH_OUTPUT, out, true);
	}

	protected void validSelectionTestChecked() throws Exception {
		performTest(fgTestSetup.getBasicPackage(), "EC", "basic_out");

	}

	public void testProject1() throws Exception {
		validSelectionTestChecked();
	}

}
