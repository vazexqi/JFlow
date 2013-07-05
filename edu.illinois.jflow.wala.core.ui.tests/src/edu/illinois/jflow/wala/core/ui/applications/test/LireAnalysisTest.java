package edu.illinois.jflow.wala.core.ui.applications.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import edu.illinois.jflow.jflow.wala.dataflowanalysis.StageInterferenceInfo;
import edu.illinois.jflow.wala.core.ui.tests.Activator;
import edu.illinois.jflow.wala.core.ui.tests.JFlowTest;

public class LireAnalysisTest extends JFlowTest {

	private static final String PROJECT_NAME= "Lire";

	private static final String PROJECT_ZIP= "lire-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public LireAnalysisTest() {
		super(PROJECT);
	}

	@Override
	protected String getTestPackageName() {
		return "edu/illinois/jflow/benchmark";
	}

	@Test
	public void testLireIndexingExample_ScalarDependencies() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "serialIndexImages", "", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 69 }, { 72, 73 }, { 77 }, { 81 }, { 85, 86 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage stage1= checker.getStage(1);
		List<DataDependence> stage1Input= stage1.getInputDataDependences();
		assertEquals(3, stage1Input.size());
		assertTrue(stage1Input.get(0).getLocalVariableNames().contains("imagePath"));
		assertTrue(stage1Input.get(1).getLocalVariableNames().contains("this"));
		assertTrue(stage1Input.get(2).getLocalVariableNames().contains("imagePath"));
		List<DataDependence> stage1Output= stage1.getOutputDataDependences();
		assertEquals(4, stage1Output.size());
		assertTrue(stage1Output.get(0).getLocalVariableNames().contains("bufferedImage"));
		assertTrue(stage1Output.get(1).getLocalVariableNames().contains("bufferedImage"));
		assertTrue(stage1Output.get(2).getLocalVariableNames().contains("bufferedImage"));
		assertTrue(stage1Output.get(3).getLocalVariableNames().contains("docJPEG"));

		PipelineStage stage2= checker.getStage(2);
		List<DataDependence> stage2Input= stage2.getInputDataDependences();
		assertEquals(4, stage2Input.size());
		assertTrue(stage2Input.get(0).getLocalVariableNames().contains("this"));
		assertTrue(stage2Input.get(1).getLocalVariableNames().contains("imagePath"));
		assertTrue(stage2Input.get(2).getLocalVariableNames().contains("docJPEG"));
		assertTrue(stage2Input.get(3).getLocalVariableNames().contains("bufferedImage"));
		List<DataDependence> stage2Output= stage2.getOutputDataDependences();
		assertEquals(1, stage2Output.size());
		assertTrue(stage2Output.get(0).getLocalVariableNames().contains("docTamura"));

		PipelineStage stage3= checker.getStage(3);
		List<DataDependence> stage3Input= stage3.getInputDataDependences();
		assertEquals(4, stage3Input.size());
		assertTrue(stage3Input.get(0).getLocalVariableNames().contains("this"));
		assertTrue(stage3Input.get(1).getLocalVariableNames().contains("imagePath"));
		assertTrue(stage3Input.get(2).getLocalVariableNames().contains("bufferedImage"));
		assertTrue(stage3Input.get(3).getLocalVariableNames().contains("docTamura"));
		List<DataDependence> stage3Output= stage3.getOutputDataDependences();
		assertEquals(1, stage3Output.size());
		assertTrue(stage3Output.get(0).getLocalVariableNames().contains("docColor"));

		PipelineStage stage4= checker.getStage(4);
		List<DataDependence> stage4Input= stage4.getInputDataDependences();
		assertEquals(5, stage4Input.size());
		assertTrue(stage4Input.get(0).getLocalVariableNames().contains("this"));
		assertTrue(stage4Input.get(1).getLocalVariableNames().contains("docColor"));
		assertTrue(stage4Input.get(2).getLocalVariableNames().contains("imagePath"));
		assertTrue(stage4Input.get(3).getLocalVariableNames().contains("bufferedImage"));
		assertTrue(stage4Input.get(4).getLocalVariableNames().contains("indexWriter"));
		List<DataDependence> stage4Output= stage4.getOutputDataDependences();
		assertEquals(0, stage4Output.size());
	}

	@Test
	public void testLireIndexingExample_HeapDependencies() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "serialIndexImages", "", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 69 }, { 72, 73 }, { 77 }, { 81 }, { 85, 86 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage generator= checker.getGenerator();

		PipelineStage stage1= checker.getStage(1);
		StageInterferenceInfo stage1Info= new StageInterferenceInfo(checker, stage1);
		stage1Info.checkInterference();
		System.out.println(stage1Info);
		printModRefInfo(stage1);

		PipelineStage stage2= checker.getStage(2);
		StageInterferenceInfo stage2Info= new StageInterferenceInfo(checker, stage2);
		stage2Info.checkInterference();
		System.out.println(stage1Info);
		printModRefInfo(stage2);

		PipelineStage stage3= checker.getStage(3);
		StageInterferenceInfo stage3Info= new StageInterferenceInfo(checker, stage3);
		stage3Info.checkInterference();
		System.out.println(stage1Info);
		printModRefInfo(stage3);

		PipelineStage stage4= checker.getStage(4);
		StageInterferenceInfo stage4Info= new StageInterferenceInfo(checker, stage4);
		stage4Info.checkInterference();
		System.out.println(stage1Info);
		printModRefInfo(stage4);
	}
}
