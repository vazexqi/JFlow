package edu.illinois.jflow.wala.core.ui.applications.test;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;

import edu.illinois.jflow.wala.core.ui.tests.Activator;
import edu.illinois.jflow.wala.core.ui.tests.JFlowTest;

@Ignore
public class Jbzip2AnalysisViewer extends JFlowTest {

	private static final String PROJECT_NAME= "Jbzip2";

	private static final String PROJECT_ZIP= "jbzip2-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public Jbzip2AnalysisViewer() {
		super(PROJECT);
	}

	@Override
	protected String getTestPackageName() {
		return "edu/illinois/jflow/benchmark";
	}

	@Test
	public void testRoundTrip_HeapDependencies() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "roundTrip", "Ljava/util/List;", "V");
		openShell();
	}

}
