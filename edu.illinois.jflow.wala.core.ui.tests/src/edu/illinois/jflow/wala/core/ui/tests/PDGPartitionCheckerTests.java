package edu.illinois.jflow.wala.core.ui.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.CancelException;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGPartitionerChecker;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PipelineStage;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;

public class PDGPartitionCheckerTests extends JFlowTest {
	private static final String PROJECT_NAME= "edu.illinois.jflow.test.data";

	private static final String PROJECT_ZIP= "test-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public PDGPartitionCheckerTests() {
		super(PROJECT);
	}

	@Override
	String getTestPackageName() {
		return "partitionchecker";
	}

	//////////
	// Tests
	// There is a special naming convention here that must be obeyed
	// The name of the method corresponds to the Java class file that we want to test
	// i.e., testBlah_whatever looks for a class Blah
	// You can use the _whatever part to distinguish different tests

	protected String getTestName() {
		StackTraceElement stack[]= new Throwable().getStackTrace();
		for (int i= 0; i <= stack.length; i++) {
			String methodName= stack[i].getMethodName();
			if (methodName.startsWith("test")) {
				// Filter out the _part
				int indexOfUnderscore= methodName.indexOf("_");
				if (indexOfUnderscore != -1) {
					return methodName.substring(0, indexOfUnderscore);
				} else
					return methodName;
			}
		}

		throw new Error("test method not found");
	}

	@Test
	public void testProject2_checkPartitionsWithoutHeapAnalysis() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 20 }, { 23 }, { 27 }, { 31, 32 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		// The following are quick sanity checks. They don't test in detail, nor should they.
		// The deeper tests are done in the PDGExtractClosureAnalyzerTests

		/*
		Generator
		[ -- <Primordial,Ljava/util/ArrayList> [data] -->]
		[ -- <Source,Lpartitionchecker/Datum> [d] -->,  -- <Source,Lpartitionchecker/Datum> [d] -->,  -- <Source,Lpartitionchecker/Datum> [d] -->]
		[d]

		Stage1
		[ -- <Source,Lpartitionchecker/Datum> [d] -->]
		[ -- <Primordial,I> [field] -->]
		[field]

		Stage2
		[ -- <Primordial,I> [field] -->]
		[ -- <Primordial,I> [manipulatedField] -->]
		[manipulatedField]

		Stage3
		[ -- <Source,Lpartitionchecker/Datum> [d] -->,  -- <Primordial,I> [manipulatedField] -->,  -- <Source,Lpartitionchecker/Datum> [d] -->]
		[]
		[]
		 */

		// Check stage0, i.e., Generator
		PipelineStage generator= checker.getGenerator();
		assertTrue(generator.getInputDataDependences().size() == 1); // Uses the data local variable
		assertTrue(generator.getOutputDataDependences().size() == 2); // Provides d to the following stages
		assertTrue(generator.getClosureLocalVariableNames().size() == 1);// Defines d

		// Check stage1
		PipelineStage stage1= checker.getStage(1);
		assertTrue(stage1.getInputDataDependences().size() == 1); // Uses d
		assertTrue(stage1.getOutputDataDependences().size() == 1); // Provides field
		assertTrue(stage1.getClosureLocalVariableNames().size() == 1); // Defines field

		// Check stage2
		PipelineStage stage2= checker.getStage(2);
		assertTrue(stage2.getInputDataDependences().size() == 1); // Uses field
		assertTrue(stage2.getOutputDataDependences().size() == 1); // Provides manipulatedField
		assertTrue(stage2.getClosureLocalVariableNames().size() == 1); // Defines manipulatedField

		// Check stage3
		PipelineStage stage3= checker.getStage(3);
		assertTrue(stage3.getInputDataDependences().size() == 2); // Uses d, d (again) and manipulatedField
		assertTrue(stage3.getOutputDataDependences().size() == 0);
		assertTrue(stage3.getClosureLocalVariableNames().size() == 0);
	}

	@Test
	public void testProject3_checkPartitionsWithoutHeapAnalysis() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 16 }, { 19 }, { 23 }, { 27, 28 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		// The following are quick sanity checks. They don't test in detail, nor should they.
		// The deeper tests are done in the PDGExtractClosureAnalyzerTests

		/*
		Generator
		[ -- <Source,[Lpartitionchecker/Datum> [data] -->]
		[ -- <Primordial,I> [i] -->,  -- <Primordial,I> [i] -->,  -- <Primordial,I> [i] -->]
		[i]

		Stage1
		[ -- <Source,[Lpartitionchecker/Datum> [data] -->,  -- <Primordial,I> [i] -->]
		[ -- <Primordial,I> [field] -->]
		[field]

		Stage2
		[ -- <Primordial,I> [field] -->]
		[ -- <Primordial,I> [manipulatedField] -->]
		[manipulatedField]

		Stage3
		[ -- <Primordial,I> [manipulatedField] -->,  -- <Source,[Lpartitionchecker/Datum> [data] -->,  -- <Primordial,I> [i] -->,  -- <Source,[Lpartitionchecker/Datum> [data] -->,  -- <Primordial,I> [i] -->]
		[]
		[]
		 */

		// Check stage0, i.e., Generator
		PipelineStage generator= checker.getGenerator();
		assertTrue(generator.getInputDataDependences().size() == 1); // Uses the data local variable
		assertTrue(generator.getOutputDataDependences().size() == 3); // Provides d to the following stages
		assertTrue(generator.getClosureLocalVariableNames().size() == 1);// Defines d

		// Check stage1
		PipelineStage stage1= checker.getStage(1);
		assertTrue(stage1.getInputDataDependences().size() == 2); // Uses i, data
		assertTrue(stage1.getOutputDataDependences().size() == 1); // Provides field
		assertTrue(stage1.getClosureLocalVariableNames().size() == 1); // Defines field

		// Check stage2
		PipelineStage stage2= checker.getStage(2);
		assertTrue(stage2.getInputDataDependences().size() == 1); // Uses field
		assertTrue(stage2.getOutputDataDependences().size() == 1); // Provides manipulatedField
		assertTrue(stage2.getClosureLocalVariableNames().size() == 1); // Defines manipulatedField

		// Check stage3
		PipelineStage stage3= checker.getStage(3);
		assertTrue(stage3.getInputDataDependences().size() == 5); // Uses data, data (again), manipulatedField, i and i(again)
		assertTrue(stage3.getOutputDataDependences().size() == 0);
		assertTrue(stage3.getClosureLocalVariableNames().size() == 0);
	}

	@Test
	public void testProject2_checkLoopCarriedDependency() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 20 }, { 23 }, { 27 }, { 31, 32 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		assertFalse(checker.containsLoopCarriedDependency());

	}

	@Test
	public void testProject2LoopCarriedDependency_checkLoopCarriedDependency() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 22 }, { 25 }, { 29 }, { 33, 34 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		assertTrue(checker.containsLoopCarriedDependency());
	}


	@Test
	public void testProject3_checkLoopCarriedDependency() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 16 }, { 19 }, { 23 }, { 27, 28 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		assertFalse(checker.containsLoopCarriedDependency());
	}

	// Illustrate that Wala doesn't properly connect java.lang.Integer pointer variables with instance keys
	@Test
	public void testProject0_checkHeapAnalysis() throws IOException, InvalidClassFileException, CancelException {
		IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 7, 8, 9, 10 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage generator= checker.getGenerator();
		assertTrue(generator.getRefs().size() == 2); // There are two references to fields: p1.field and p2.field but they are to Integer
		assertTrue(generator.getMods().size() == 0);
	}

	// Illustrate that Wala handles java.lang.Object types properly for pointerkey <-> instancekey
	@Test
	public void testProject0a_checkHeapAnalysis() throws IOException, InvalidClassFileException, CancelException {
		IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 7, 8, 9, 10 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage generator= checker.getGenerator();
		assertTrue(generator.getRefs().size() == 2); // There are two references to fields: p1.field and p2.field
		assertTrue(generator.getMods().size() == 0);
	}

	/**
	 * Project1 directly accesses heap variables without method calls - so we are testing for direct
	 * accesses to heap
	 */
	@Test
	public void testProject1_checkHeapAnalysis() throws IOException, InvalidClassFileException, CancelException {
		IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 18 }, { 21 }, { 25 }, { 29 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage generator= checker.getGenerator();
		//TODO: Verify this later – the generator is more complicated because of how many references it accesss

		// Check stage1
		PipelineStage stage1= checker.getStage(1);
		assertTrue(stage1.getRefs().size() == 1);
		assertTrue(stage1.getMods().size() == 0);

		// Check stage2
		PipelineStage stage2= checker.getStage(2);
		assertTrue(stage2.getRefs().size() == 0);
		assertTrue(stage2.getMods().size() == 0);

		// Check stage3
		PipelineStage stage3= checker.getStage(3);
		assertTrue(stage3.getRefs().size() == 0);
		assertTrue(stage3.getMods().size() == 1);
	}

	/**
	 * Project1 directly accesses heap variables without method calls - so we are testing for direct
	 * accesses to heap
	 */
	@Test
	public void testProject1a_checkHeapAnalysis() throws IOException, InvalidClassFileException, CancelException {
		IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 23 }, { 26 }, { 30 }, { 34 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage generator= checker.getGenerator();
		//TODO: Verify this later – the generator is more complicated because of how many references it accesss

		// Check stage1
		PipelineStage stage1= checker.getStage(1);
		assertTrue(stage1.getRefs().size() == 4);
		assertTrue(stage1.getMods().size() == 0);

		// Check stage2
		PipelineStage stage2= checker.getStage(2);
		assertTrue(stage2.getRefs().size() == 0);
		assertTrue(stage2.getMods().size() == 0);

		// Check stage3
		PipelineStage stage3= checker.getStage(3);
		assertTrue(stage3.getRefs().size() == 0);
		assertTrue(stage3.getMods().size() == 4);
	}


	/**
	 * This test verifies that we can track mod/ref interprocedurally
	 */
	@Test
	public void testProject4_checkHeapAnalysis() throws IOException, InvalidClassFileException, CancelException {
		IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "entry", "[Lpartitionchecker/Datum;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
		List<List<Integer>> selections= selectionFromArray(new int[][] { { 11 }, { 14 }, { 18 }, { 22 } });
		PDGPartitionerChecker checker= PDGPartitionerChecker.makePartitionChecker(pdg, selections);

		checker.computeHeapDependency(callGraph, engine.getPointerAnalysis());

		PipelineStage generator= checker.getGenerator();
		//TODO: Verify this later – the generator is more complicated because of how many references it accesss

		// Check stage1
		PipelineStage stage1= checker.getStage(1);
		assertTrue(stage1.getRefs().size() == 4);
		assertTrue(stage1.getMods().size() == 0);

		// Check stage2
		PipelineStage stage2= checker.getStage(2);
		assertTrue(stage2.getRefs().size() == 4);
		assertTrue(stage2.getMods().size() == 0);

		// Check stage3
		PipelineStage stage3= checker.getStage(3);
		assertTrue(stage3.getRefs().size() == 0);
		assertTrue(stage3.getMods().size() == 4);
	}

	protected List<List<Integer>> selectionFromArray(int[][] lines) {
		List<List<Integer>> selections= new ArrayList<List<Integer>>();
		for (int[] stageLines : lines) {
			// int is a primitive and Arrays.asList(stageLines) doesn't do the right thing
			// it produces List<int[]> which is not what I want
			List<Integer> stageLine= new ArrayList<Integer>();
			for (int line : stageLines) {
				stageLine.add(line);
			}
			selections.add(stageLine);
		}
		return selections;
	}
}
