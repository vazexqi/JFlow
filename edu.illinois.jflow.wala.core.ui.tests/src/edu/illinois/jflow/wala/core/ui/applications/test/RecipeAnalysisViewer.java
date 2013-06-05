package edu.illinois.jflow.wala.core.ui.applications.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;

import edu.illinois.jflow.wala.core.ui.tests.Activator;
import edu.illinois.jflow.wala.core.ui.tests.JFlowTest;

public class RecipeAnalysisViewer extends JFlowTest {
	private static final String PROJECT_NAME= "edu.illinois.jflow.test.recipe";

	private static final String PROJECT_ZIP= "recipe-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public RecipeAnalysisViewer() {
		super(PROJECT);
	}

	@Override
	protected String getTestPackageName() {
		return "recipe";
	}

	@Test
	public void testRecipe() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		openShell();
	}

}
