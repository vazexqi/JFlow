package edu.illinois.jflow.wala.javaviewer;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;

import edu.illinois.jflow.wala.core.ui.tests.Activator;
import edu.illinois.jflow.wala.core.ui.tests.JFlowTest;

/*
 * This class doesn't really test anything. I'm just abusing the infrastructure that we have set up
 * so we can launch a GUI to visualize where the pointers are.
 */
public class PDGPartitionViewer extends JFlowTest {
	private static final String PROJECT_NAME= "edu.illinois.jflow.test.data";

	private static final String PROJECT_ZIP= "test-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public PDGPartitionViewer() {
		super(PROJECT);
	}

	@Override
	protected String getTestPackageName() {
		return "partitionchecker";
	}

	//////////
	// Tests
	// There is a special naming convention here that must be obeyed
	// The name of the method corresponds to the Java class file that we want to test
	// i.e., testBlah looks for a class Blah

	@Test
	public void testProject5() throws IllegalArgumentException, IOException, CancelException, InterruptedException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		openShell();
	}
}
