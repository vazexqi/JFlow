package edu.illinois.jflow.wala.core.ui.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import com.ibm.wala.cast.java.test.JDTJavaTest;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Iterator2Iterable;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGNode;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;

public class PDGTests extends JDTJavaTest {

	private static final String TEST_PACKAGE_NAME= "pdg";

	private static final String PROJECT_NAME= "edu.illinois.jflow.test.data";

	private static final String PROJECT_ZIP= "test-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	private AbstractAnalysisEngine engine;

	public PDGTests() {
		super(PROJECT);
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

			// Verify
			assertEquals("Number of nodes not expected", 2, pdg.getNumberOfNodes());
			for (PDGNode pdgNode : Iterator2Iterable.make(pdg.iterator())) {
				// Should have no edges
				// There are no edges even though there are dependencies in the source code because of copy propagation being performed.
				// TODO: Again, investigate if this is going to be a problem
				assertTrue(pdg.getPredNodeCount(pdgNode) == 0);
				assertTrue(pdg.getSuccNodeCount(pdgNode) == 0);
			}


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

			// Verify
			assertEquals("Number of nodes not expected", 4, pdg.getNumberOfNodes());

			// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
			PDGNode methodParam= pdg.getNode(0);
			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode produceC= pdg.getNode(3);

			Set<? extends String> aToB= pdg.getEdgeLabels(produceA, produceB);
			assertEquals("There should only be one edge", 1, aToB.size());
			assertTrue("The dependency edge a -> b is missing", aToB.contains("<Primordial,I> [a]"));

			Set<? extends String> bToC= pdg.getEdgeLabels(produceB, produceC);
			assertEquals("There should only be one edge", 1, bToC.size());
			assertTrue("The dependency edge b -> c is missing", bToC.contains("<Primordial,I> [b]"));

			// Should have no edges
			assertTrue(pdg.getPredNodeCount(methodParam) == 0);
			assertTrue(pdg.getSuccNodeCount(methodParam) == 0);

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

			// Verify
			assertEquals("Number of nodes not expected", 4, pdg.getNumberOfNodes());

			// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
			PDGNode methodParam= pdg.getNode(0);
			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode produceC= pdg.getNode(3);

			Set<? extends String> aToB= pdg.getEdgeLabels(produceA, produceB);
			assertEquals("There should only be one edge", 1, aToB.size());
			assertTrue("The dependency edge a -> b is missing", aToB.contains("<Primordial,I> [a]"));

			Set<? extends String> aToC= pdg.getEdgeLabels(produceA, produceC);
			assertEquals("There should only be one edge", 1, aToC.size());
			assertTrue("The dependency edge a -> c is missing", aToC.contains("<Primordial,I> [a]"));

			// Should have no edges
			assertTrue(pdg.getPredNodeCount(methodParam) == 0);
			assertTrue(pdg.getSuccNodeCount(methodParam) == 0);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks what happens when we have a statement that modifies a value (e.g., a += 2;). In SSA
	 * world a new variable is created so this test is to check that we can still accurately check
	 * the dependency.
	 */
	@Test
	public void testProject4() {
		try {
			IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());

			// Verify
			assertEquals("Number of nodes not expected", 5, pdg.getNumberOfNodes());

			// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
			PDGNode methodParam= pdg.getNode(0);
			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode modifyA= pdg.getNode(3);
			PDGNode produceC= pdg.getNode(4);

			Set<? extends String> aToB= pdg.getEdgeLabels(produceA, produceB);
			assertEquals("There should only be one edge", 1, aToB.size());
			assertTrue("The dependency edge a -> b is missing", aToB.contains("<Primordial,I> [a]"));

			Set<? extends String> aToModifyA= pdg.getEdgeLabels(produceA, modifyA);
			assertEquals("There should only be one edge", 1, aToModifyA.size());
			assertTrue("The dependency edge a -> modifyA is missing", aToB.contains("<Primordial,I> [a]"));

			Set<? extends String> modifyAToC= pdg.getEdgeLabels(modifyA, produceC);
			assertEquals("There should only be one edge", 1, modifyAToC.size());
			assertTrue("The dependency edge modifyA -> c is missing", modifyAToC.contains("<Primordial,I> [a]"));

			// Should have no edges
			assertTrue(pdg.getPredNodeCount(methodParam) == 0);
			assertTrue(pdg.getSuccNodeCount(methodParam) == 0);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tests dependencies to a simple method parameter
	 */
	@Test
	public void testProject5() {
		try {
			IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "entry", "I", "V");
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());

			// Verify
			assertEquals("Number of nodes not expected", 4, pdg.getNumberOfNodes());

			// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
			PDGNode param= pdg.getNode(0);
			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode produceC= pdg.getNode(3);

			Set<? extends String> paramToA= pdg.getEdgeLabels(param, produceA);
			assertEquals("There should only be one edge", 1, paramToA.size());
			assertTrue("The dependency edge param -> a is missing", paramToA.contains("<Primordial,I> [param]"));

			Set<? extends String> aToB= pdg.getEdgeLabels(produceA, produceB);
			assertEquals("There should only be one edge", 1, aToB.size());
			assertTrue("The dependency edge a -> b is missing", aToB.contains("<Primordial,I> [a]"));

			Set<? extends String> bToC= pdg.getEdgeLabels(produceB, produceC);
			assertEquals("There should only be one edge", 1, bToC.size());
			assertTrue("The dependency edge b -> c is missing", bToC.contains("<Primordial,I> [b]"));

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tests dependencies to a container method parameter
	 */
	@Test
	public void testProject6() {
		try {
			IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "entry", "Ljava/util/List;", "V");
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());

			// Verify
			assertEquals("Number of nodes not expected", 4, pdg.getNumberOfNodes());

			// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
			PDGNode param= pdg.getNode(0);
			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode produceC= pdg.getNode(3);

			Set<? extends String> paramToA= pdg.getEdgeLabels(param, produceA);
			assertEquals("There should only be one edge", 1, paramToA.size());
			assertTrue("The dependency edge param -> a is missing", paramToA.contains("<Primordial,Ljava/util/List> [param]"));

			// Contains a self loop since we need to retrieve values from the parameter using get(0)
			Set<? extends String> aToA= pdg.getEdgeLabels(produceA, produceA);
			assertEquals("There should only be one edge", 2, aToA.size());
			// This means there is a dependency but it with an internal temp variable that we don't care about
			assertTrue("The dependency edge a -> b is missing", aToA.contains("<Primordial,Ljava/lang/Object> []"));
			assertTrue("The dependency edge a -> b is missing", aToA.contains("<Primordial,Ljava/lang/Integer> []"));

			Set<? extends String> aToB= pdg.getEdgeLabels(produceA, produceB);
			assertEquals("There should only be one edge", 1, aToB.size());
			assertTrue("The dependency edge a -> b is missing", aToB.contains("<Primordial,I> [a]"));

			Set<? extends String> bToC= pdg.getEdgeLabels(produceB, produceC);
			assertEquals("There should only be one edge", 1, bToC.size());
			assertTrue("The dependency edge b -> c is missing", bToC.contains("<Primordial,I> [b]"));

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Tests dependencies to a heap object.
	 * 
	 * TODO: This is the current behavior but we might want to change how this works to handle
	 * forwarding.
	 */
	@Test
	public void testProject7() {
		try {
			IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());

			// Verify
			assertEquals("Number of nodes not expected", 6, pdg.getNumberOfNodes());

			// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
			PDGNode param= pdg.getNode(0);
			PDGNode createList= pdg.getNode(1);
			PDGNode produceA= pdg.getNode(2);
			PDGNode addA= pdg.getNode(3);
			PDGNode produceB= pdg.getNode(4);
			PDGNode addB= pdg.getNode(5);

			Set<? extends String> createListToAddA= pdg.getEdgeLabels(createList, addA);
			assertEquals("There should only be one edge", 1, createListToAddA.size());
			assertTrue("The dependency edge createList -> addA is missing", createListToAddA.contains("<Primordial,Ljava/util/ArrayList> [list]"));

			Set<? extends String> createListToAddB= pdg.getEdgeLabels(createList, addB);
			assertEquals("There should only be one edge", 1, createListToAddB.size());
			assertTrue("The dependency edge createList -> addB is missing", createListToAddB.contains("<Primordial,Ljava/util/ArrayList> [list]"));

			Set<? extends String> aToB= pdg.getEdgeLabels(produceA, produceB);
			assertEquals("There should only be one edge", 1, aToB.size());
			assertTrue("The dependency edge a -> b is missing", aToB.contains("<Primordial,I> [a]"));

			Set<? extends String> aToAddA= pdg.getEdgeLabels(produceA, addA);
			assertEquals("There should only be one edge", 1, aToAddA.size());
			assertTrue("The dependency edge a -> addA is missing", aToAddA.contains("<Primordial,I> [a]"));

			Set<? extends String> bToAddB= pdg.getEdgeLabels(produceB, addB);
			assertEquals("There should only be one edge", 1, bToAddB.size());
			assertTrue("The dependency edge b -> addB is missing", bToAddB.contains("<Primordial,I> [b]"));

			// Should have no edges
			assertTrue(pdg.getPredNodeCount(param) == 0);
			assertTrue(pdg.getSuccNodeCount(param) == 0);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}

	//////////////////
	// Utility Methods
	//////////////////

	private String constructFullyQualifiedClass() {
		return TEST_PACKAGE_NAME + "/" + singleInputForTest();
	}

	private IR retrieveMethodToBeInspected(String fullyQualifiedClassName, String methodName, String methodParameters, String returnType) throws IOException {
		engine= getAnalysisEngine(simplePkgTestEntryPoint(TEST_PACKAGE_NAME), rtJar);
		engine.buildAnalysisScope();
		IClassHierarchy classHierarchy= engine.buildClassHierarchy();

		MethodReference methodRef= descriptorToMethodRef(String.format("Source#%s#%s#(%s)%s", fullyQualifiedClassName, methodName, methodParameters, returnType), classHierarchy);
		IMethod method= classHierarchy.resolveMethod(methodRef);
		return engine.getCache().getSSACache().findOrCreateIR(method, Everywhere.EVERYWHERE, new AnalysisOptions().getSSAOptions());
	}

}
