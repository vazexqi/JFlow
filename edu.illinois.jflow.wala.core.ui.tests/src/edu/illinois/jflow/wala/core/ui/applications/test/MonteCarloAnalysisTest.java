package edu.illinois.jflow.wala.core.ui.applications.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.DataDependence;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGExtractClosureAnalyzer;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGPartitionerChecker;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PipelineStage;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.StageInterferenceInfo;
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
		return "edu/illinois/jflow/benchmark";
	}

	@Ignore
	@Test
	public void testJGFMonteCarloBench_ScalarDependencies() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass("AppDemo"), "runSerial", "", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 168 }, { 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186 },
				{ 188, 189, 190, 191, 192, 193, 194, 195 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage stage1= checker.getStage(1);

		List<DataDependence> stage1Input= stage1.getInputDataDependences();
		Set<String> stage1InputNames= PDGExtractClosureAnalyzer.extractNamesFromDependencies(stage1Input);
		stage1InputNames.remove(PDGExtractClosureAnalyzer.THIS_PARAMETER);
		assertTrue(stage1InputNames.contains("ilow")); // ilow really shouldn't be there but it is due to false information from WALA's copy propagation
		assertTrue(stage1InputNames.contains("iRun"));

		List<DataDependence> stage1Output= stage1.getOutputDataDependences();
		Set<String> stage1OutputNames= PDGExtractClosureAnalyzer.extractNamesFromDependencies(stage1Output);
		stage1OutputNames.remove(PDGExtractClosureAnalyzer.THIS_PARAMETER);
		assertTrue(stage1OutputNames.contains("iupper"));
		assertTrue(stage1OutputNames.contains("psArray"));

		PipelineStage stage2= checker.getStage(2);

		List<DataDependence> stage2Input= stage2.getInputDataDependences();
		Set<String> stage2InputNames= PDGExtractClosureAnalyzer.extractNamesFromDependencies(stage2Input);
		stage2InputNames.remove(PDGExtractClosureAnalyzer.THIS_PARAMETER);
		assertTrue(stage2InputNames.contains("ilow"));
		assertTrue(stage2InputNames.contains("iRun"));
		assertTrue(stage2InputNames.contains("iupper"));
		assertTrue(stage2InputNames.contains("psArray"));

		List<DataDependence> stage2Output= stage2.getOutputDataDependences();
		Set<String> stage2OutputNames= PDGExtractClosureAnalyzer.extractNamesFromDependencies(stage2Output);
		stage2OutputNames.remove(PDGExtractClosureAnalyzer.THIS_PARAMETER);
		assertEquals(0, stage2OutputNames.size());

		System.err.println("Pause");

	}

	@Test
	public void testJGFMonteCarloBench_HeapDependencies() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass("AppDemo"), "runSerial", "", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 168 }, { 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186 },
				{ 188, 189, 190, 191, 192, 193, 194, 195 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());
		checker.checkInterference();

		if (checker.hasInterference()) {
			for (String message : checker.getInterferenceMessages()) {
				System.out.println(message);
				System.out.println();
			}
		}

		PipelineStage stage1= checker.getStage(1);
		StageInterferenceInfo stage1Info= new StageInterferenceInfo(checker, stage1);
		stage1Info.checkInterference();
		System.out.println(stage1Info);
		printModRefInfo(stage1);

		PipelineStage stage2= checker.getStage(2);
		StageInterferenceInfo stage2Info= new StageInterferenceInfo(checker, stage2);
		stage2Info.checkInterference();
		System.out.println(stage2Info);
		printModRefInfo(stage2);
	}
}
