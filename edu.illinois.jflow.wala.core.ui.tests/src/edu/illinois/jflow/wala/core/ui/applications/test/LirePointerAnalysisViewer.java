package edu.illinois.jflow.wala.core.ui.applications.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;

import edu.illinois.jflow.wala.core.ui.tests.Activator;
import edu.illinois.jflow.wala.core.ui.tests.JFlowTest;

/*
 * This class doesn't really test anything. I'm just abusing the infrastructure that we have set up
 * so we can launch a GUI to visualize where the pointers are.
 */
public class LirePointerAnalysisViewer extends JFlowTest {

	private static final String PROJECT_NAME= "Lire";

	private static final String PROJECT_ZIP= "lire-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public LirePointerAnalysisViewer() {
		super(PROJECT);
	}

	@Override
	protected String getTestPackageName() {
		return "edu/illinois/jflow/benchmark";
	}

	@Test
	public void testLireIndexingExample() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		// This line is just to get the analysis started
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "serialIndexImages", "", "V");
		openShell();
	}

}
