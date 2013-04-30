package edu.illinois.jflow.wala.core.ui.applications.test;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.DataDependence;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGPartitionerChecker;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PipelineStage;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;
import edu.illinois.jflow.wala.core.ui.tests.Activator;
import edu.illinois.jflow.wala.core.ui.tests.JFlowTest;

public class MonteCarloAnalysisTest extends JFlowTest {

	private static final String PROJECT_NAME= "MonteCarlo";

	private static final String PROJECT_ZIP= "montecarlo-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public MonteCarloAnalysisTest() {
		super(PROJECT);
	}

	@Override
	protected String getTestPackageName() {
		return "";
	}

	@Test
	public void testJGFMonteCarloBench_ScalarDependencies() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "runSerial", "", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 167 }, { 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185 },
				{ 187, 188, 189, 190, 191, 192, 193, 194 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage stage1= checker.getStage(1);
		List<DataDependence> stage1Input= stage1.getInputDataDependences();
		List<DataDependence> stage1Output= stage1.getOutputDataDependences();

		PipelineStage stage2= checker.getStage(2);
		List<DataDependence> stage2Input= stage2.getInputDataDependences();
		List<DataDependence> stage2Output= stage2.getOutputDataDependences();

		System.err.println("Pause");

	}

}
