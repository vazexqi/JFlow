package edu.illinois.jflow.wala.core.ui.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
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
			assertEquals("There should not be any input dependencies", 1, analyzer.getInputDataDependences().size());
			assertEquals("There should not be any output dependencies", 1, analyzer.getOutputDataDependences().size());

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
}
