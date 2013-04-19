package edu.illinois.jflow.wala.core.ui.applications.test;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;

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
	public void testRoundTrip() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "roundTrip", "Ljava/util/List;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 96 }, { 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110 }, { 114, 115, 116, 117, 118, 119, 120, 121 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage stage1= checker.getStage(1);
		printModRefInfo(stage1);

		PipelineStage stage2= checker.getStage(2);
		printModRefInfo(stage2);
	}

	private void printModRefInfo(PipelineStage stage) {
		System.err.println("<<<REF>>>");
		System.err.println(stage.getPrettyPrintRefs());

		System.err.println("<<<MOD>>>");
		System.err.println(stage.getPrettyPrintMods());

		System.err.println("<<IGNORED>>");
		System.err.println(stage.getPrettyPrintIgnored());
	}
}
