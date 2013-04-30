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
import edu.illinois.jflow.wala.core.ui.tests.Activator;
import edu.illinois.jflow.wala.core.ui.tests.JFlowTest;

public class Jbzip2AnalysisTest extends JFlowTest {
	private static final String PROJECT_NAME= "Jbzip2";

	private static final String PROJECT_ZIP= "jbzip2-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public Jbzip2AnalysisTest() {
		super(PROJECT);
	}

	@Override
	protected String getTestPackageName() {
		return "edu/illinois/jflow/benchmark";
	}

	@Test
	public void testRoundTrip_ScalarDependencies() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "roundTrip", "Ljava/util/List;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 95 }, { 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110 }, { 114, 115, 116, 117, 118, 119, 120, 121 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage stage1= checker.getStage(1);
		List<DataDependence> stage1Input= stage1.getInputDataDependences();
		assertEquals(1, stage1Input.size());
		assertTrue(stage1Input.get(0).getLocalVariableNames().contains("inputFile"));
		List<DataDependence> stage1Output= stage1.getOutputDataDependences();
		assertEquals(1, stage1Output.size());
		assertTrue(stage1Output.get(0).getLocalVariableNames().contains("tempFile"));

		PipelineStage stage2= checker.getStage(2);
		List<DataDependence> stage2Input= stage2.getInputDataDependences();
		assertEquals(1, stage2Input.size());
		assertTrue(stage2Input.get(0).getLocalVariableNames().contains("tempFile"));
		List<DataDependence> stage2Output= stage2.getOutputDataDependences();
		assertEquals(0, stage2Output.size());
	}

	@Test
	public void testRoundTrip_HeapDependencies() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "roundTrip", "Ljava/util/List;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 95 }, { 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110 }, { 114, 115, 116, 117, 118, 119, 120, 121 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage stage1= checker.getStage(1);
		printModRefInfo(stage1);

		PipelineStage stage2= checker.getStage(2);
		printModRefInfo(stage2);
	}
}
