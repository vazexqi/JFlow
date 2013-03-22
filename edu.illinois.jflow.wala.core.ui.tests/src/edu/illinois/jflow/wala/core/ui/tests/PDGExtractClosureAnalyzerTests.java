package edu.illinois.jflow.wala.core.ui.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.TypeReference;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.DataDependence;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGExtractClosureAnalyzer;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGNode;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;

public class PDGExtractClosureAnalyzerTests extends JFlowTest {
	private static final String PROJECT_NAME= "edu.illinois.jflow.test.data";

	private static final String PROJECT_ZIP= "test-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public PDGExtractClosureAnalyzerTests() {
		super(PROJECT);
	}

	@Override
	String getTestPackageName() {
		return "analyzer";
	}

	//////////
	// Tests
	// There is a special naming convention here that must be obeyed
	// The name of the method corresponds to the Java class file that we want to test
	// i.e., testBlah looks for a class Blah

	@Test
	public void testProject1() {
		try {
			IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
			List<Integer> lines= new ArrayList<Integer>();
			lines.add(6);
			PDGExtractClosureAnalyzer analyzer= new PDGExtractClosureAnalyzer(pdg, lines);
			analyzer.analyzeSelection();

			// Verify
			assertEquals("There should not be any input dependencies", 0, analyzer.getInputDataDependences().size());
			assertEquals("There should not be any output dependencies", 0, analyzer.getOutputDataDependences().size());

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testProject2() {
		try {
			IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
			List<Integer> lines= new ArrayList<Integer>();
			lines.add(6);
			PDGExtractClosureAnalyzer analyzer= new PDGExtractClosureAnalyzer(pdg, lines);
			analyzer.analyzeSelection();

			// Verify
			assertEquals("There should be 1 input dependencies", 1, analyzer.getInputDataDependences().size());
			assertEquals("There should be 1 output dependencies", 1, analyzer.getOutputDataDependences().size());

			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode produceC= pdg.getNode(3);

			DataDependence input= analyzer.getInputDataDependences().get(0);
			DataDependence expectedInput= new DataDependence(produceA, produceB, TypeReference.Int, "[a]");
			assertEquals("a->b input dependence doesn't match", expectedInput, input);

			DataDependence output= analyzer.getOutputDataDependences().get(0);
			DataDependence expectedOutput= new DataDependence(produceB, produceC, TypeReference.Int, "[b]");
			assertEquals("b->c input dependence doesn't match", expectedOutput, output);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testProject3() {
		try {
			IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
			List<Integer> lines= Arrays.asList(6, 7);
			PDGExtractClosureAnalyzer analyzer= new PDGExtractClosureAnalyzer(pdg, lines);
			analyzer.analyzeSelection();

			// Verify
			assertEquals("There should 2 input dependencies", 2, analyzer.getInputDataDependences().size());
			assertEquals("There should 1 output dependencies", 1, analyzer.getOutputDataDependences().size());

			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode plusEqualsA= pdg.getNode(3);
			PDGNode produceC= pdg.getNode(4);

			// This one is tricky and I want to check that it is right. 
			// Basically all the dependencies are on 'a' but 'a' has been modified several times
			DataDependence input1= analyzer.getInputDataDependences().get(0);
			DataDependence expectedInput1= new DataDependence(produceA, produceB, TypeReference.Int, "[a]");
			assertEquals("a->b input dependence doesn't match", expectedInput1, input1);

			DataDependence input2= analyzer.getInputDataDependences().get(1);
			DataDependence expectedInput2= new DataDependence(produceA, plusEqualsA, TypeReference.Int, "[a]");
			assertEquals("a->a+= input dependence doesn't match", expectedInput2, input2);

			DataDependence output= analyzer.getOutputDataDependences().get(0);
			DataDependence expectedOutput= new DataDependence(plusEqualsA, produceC, TypeReference.Int, "[a]");
			assertEquals("a+=->c input dependence doesn't match", expectedOutput, output);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}

	/*
	 * This test is similar to testProject3 except that it illustrates the fact that Wala's internal representation
	 * ignores declaration statements, e.g., int a;
	 */
	@Test
	public void testProject4() {
		try {
			IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
			List<Integer> lines= Arrays.asList(10, 11);
			PDGExtractClosureAnalyzer analyzer= new PDGExtractClosureAnalyzer(pdg, lines);
			analyzer.analyzeSelection();

			// Verify
			assertEquals("There should 2 input dependencies", 2, analyzer.getInputDataDependences().size());
			assertEquals("There should 1 output dependencies", 1, analyzer.getOutputDataDependences().size());

			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode plusEqualsA= pdg.getNode(3);
			PDGNode produceC= pdg.getNode(4);

			// This one is tricky and I want to check that it is right. 
			// Basically all the dependencies are on 'a' but 'a' has been modified several times
			DataDependence input1= analyzer.getInputDataDependences().get(0);
			DataDependence expectedInput1= new DataDependence(produceA, produceB, TypeReference.Int, "[a]");
			assertEquals("a->b input dependence doesn't match", expectedInput1, input1);

			DataDependence input2= analyzer.getInputDataDependences().get(1);
			DataDependence expectedInput2= new DataDependence(produceA, plusEqualsA, TypeReference.Int, "[a]");
			assertEquals("a->a+= input dependence doesn't match", expectedInput2, input2);

			DataDependence output= analyzer.getOutputDataDependences().get(0);
			DataDependence expectedOutput= new DataDependence(plusEqualsA, produceC, TypeReference.Int, "[a]");
			assertEquals("a+=->c input dependence doesn't match", expectedOutput, output);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}
}
