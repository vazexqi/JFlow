package edu.illinois.jflow.wala.core.ui.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Iterator2Iterable;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.DataDependence;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.PDGNode;
import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;

public class PDGTests extends JFlowTest {

	private static final String PROJECT_NAME= "edu.illinois.jflow.test.data";

	private static final String PROJECT_ZIP= "test-workspace.zip";

	public static final ZippedProjectData PROJECT= new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP);

	public PDGTests() {
		super(PROJECT);
	}

	@Override
	protected String getTestPackageName() {
		return "pdg";
	}

	//////////
	// Tests
	// There is a special naming convention here that must be obeyed
	// The name of the method corresponds to the Java class file that we want to test
	// i.e., testBlah looks for a class Blah

	@Test
	public void testProject1() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());

		// Verify
		assertEquals("Number of nodes not expected", 2, pdg.getNumberOfNodes());
		for (PDGNode pdgNode : Iterator2Iterable.make(pdg.iterator())) {
			// Should have no edges
			// There are no edges even though there are dependencies in the source code because of copy propagation being performed.
			assertTrue(pdg.getPredNodeCount(pdgNode) == 0);
			assertTrue(pdg.getSuccNodeCount(pdgNode) == 0);
		}
	}

	@Test
	public void testProject2() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());

		// Verify
		assertEquals("Number of nodes not expected", 4, pdg.getNumberOfNodes());

		// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
		PDGNode methodParam= pdg.getNode(0);
		PDGNode produceA= pdg.getNode(1);
		PDGNode produceB= pdg.getNode(2);
		PDGNode produceC= pdg.getNode(3);

		TypeReference intType= TypeReference.Int;

		Set<? extends DataDependence> aToB= pdg.getEdgeLabels(produceA, produceB);
		assertEquals("There should only be one edge", 1, aToB.size());
		DataDependence aToBExpected= new DataDependence(produceA, produceB, intType, "[a]");
		assertTrue("The dependency edge a -> b is missing", aToB.contains(aToBExpected));

		Set<? extends DataDependence> bToC= pdg.getEdgeLabels(produceB, produceC);
		assertEquals("There should only be one edge", 1, bToC.size());
		DataDependence bToCExpected= new DataDependence(produceB, produceC, intType, "[b]");
		assertTrue("The dependency edge b -> c is missing", bToC.contains(bToCExpected));

		// Should have no edges
		assertTrue(pdg.getPredNodeCount(methodParam) == 0);
		assertTrue(pdg.getSuccNodeCount(methodParam) == 0);
	}

	@Test
	public void testProject3() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());

		// Verify
		assertEquals("Number of nodes not expected", 4, pdg.getNumberOfNodes());

		// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
		PDGNode methodParam= pdg.getNode(0);
		PDGNode produceA= pdg.getNode(1);
		PDGNode produceB= pdg.getNode(2);
		PDGNode produceC= pdg.getNode(3);

		TypeReference intType= TypeReference.Int;

		Set<? extends DataDependence> aToB= pdg.getEdgeLabels(produceA, produceB);
		assertEquals("There should only be one edge", 1, aToB.size());
		DataDependence aToBExpected= new DataDependence(produceA, produceB, intType, "[a]");
		assertTrue("The dependency edge a -> b is missing", aToB.contains(aToBExpected));

		Set<? extends DataDependence> aToC= pdg.getEdgeLabels(produceA, produceC);
		assertEquals("There should only be one edge", 1, aToC.size());
		DataDependence aToCExpected= new DataDependence(produceA, produceC, intType, "[a]");
		assertTrue("The dependency edge a -> c is missing", aToC.contains(aToCExpected));

		// Should have no edges
		assertTrue(pdg.getPredNodeCount(methodParam) == 0);
		assertTrue(pdg.getSuccNodeCount(methodParam) == 0);
	}

	/**
	 * Checks what happens when we have a statement that modifies a value (e.g., a += 2;). In SSA
	 * world a new variable is created so this test is to check that we can still accurately check
	 * the dependency.
	 * 
	 */
	@Test
	public void testProject4() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());

		// Verify
		assertEquals("Number of nodes not expected", 5, pdg.getNumberOfNodes());

		// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
		PDGNode methodParam= pdg.getNode(0);
		PDGNode produceA= pdg.getNode(1);
		PDGNode produceB= pdg.getNode(2);
		PDGNode modifyA= pdg.getNode(3);
		PDGNode produceC= pdg.getNode(4);

		TypeReference intType= TypeReference.Int;

		Set<? extends DataDependence> aToB= pdg.getEdgeLabels(produceA, produceB);
		assertEquals("There should only be one edge", 1, aToB.size());
		DataDependence aToBExpected= new DataDependence(produceA, produceB, intType, "[a]");
		assertTrue("The dependency edge a -> b is missing", aToB.contains(aToBExpected));

		Set<? extends DataDependence> aToModifyA= pdg.getEdgeLabels(produceA, modifyA);
		assertEquals("There should only be one edge", 1, aToModifyA.size());
		DataDependence aToModifyAExpected= new DataDependence(produceA, modifyA, intType, "[a]");
		assertTrue("The dependency edge a -> modifyA is missing", aToModifyA.contains(aToModifyAExpected));

		Set<? extends DataDependence> modifyAToC= pdg.getEdgeLabels(modifyA, produceC);
		assertEquals("There should only be one edge", 1, modifyAToC.size());
		DataDependence aToCExpected= new DataDependence(modifyA, produceC, intType, "[a]");
		assertTrue("The dependency edge modifyA -> c is missing", modifyAToC.contains(aToCExpected));

		// Should have no edges
		assertTrue(pdg.getPredNodeCount(methodParam) == 0);
		assertTrue(pdg.getSuccNodeCount(methodParam) == 0);
	}

	/**
	 * Tests dependencies to a simple method parameter
	 */
	@Test
	public void testProject5() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "entry", "I", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());

		// Verify
		assertEquals("Number of nodes not expected", 4, pdg.getNumberOfNodes());

		// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
		PDGNode param= pdg.getNode(0);
		PDGNode produceA= pdg.getNode(1);
		PDGNode produceB= pdg.getNode(2);
		PDGNode produceC= pdg.getNode(3);

		TypeReference intType= TypeReference.Int;

		Set<? extends DataDependence> paramToA= pdg.getEdgeLabels(param, produceA);
		assertEquals("There should only be one edge", 1, paramToA.size());
		DataDependence paramToAExpected= new DataDependence(param, produceA, intType, "[param]");
		assertTrue("The dependency edge param -> a is missing", paramToA.contains(paramToAExpected));

		Set<? extends DataDependence> aToB= pdg.getEdgeLabels(produceA, produceB);
		assertEquals("There should only be one edge", 1, aToB.size());
		DataDependence aToBExpected= new DataDependence(produceA, produceB, intType, "[a]");
		assertTrue("The dependency edge a -> b is missing", aToB.contains(aToBExpected));

		Set<? extends DataDependence> bToC= pdg.getEdgeLabels(produceB, produceC);
		assertEquals("There should only be one edge", 1, bToC.size());
		DataDependence bToCExpected= new DataDependence(produceB, produceC, intType, "[b]");
		assertTrue("The dependency edge b -> c is missing", bToC.contains(bToCExpected));
	}

	/**
	 * Tests dependencies to a container method parameter
	 */
	@Test
	public void testProject6() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "entry", "Ljava/util/List;", "V");
		ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());

		// Verify
		assertEquals("Number of nodes not expected", 4, pdg.getNumberOfNodes());

		// The order of building the nodes is deterministic so we can rely on the nodes being numbered in this manner
		PDGNode param= pdg.getNode(0);
		PDGNode produceA= pdg.getNode(1);
		PDGNode produceB= pdg.getNode(2);

		PDGNode produceC= pdg.getNode(3);

		TypeReference primitiveInttype= TypeReference.Int;
		TypeReference listType= TypeReference.JavaUtilList;
		TypeReference objectType= TypeReference.JavaLangObject;
		TypeReference integerType= TypeReference.JavaLangInteger;

		Set<? extends DataDependence> paramToA= pdg.getEdgeLabels(param, produceA);
		assertEquals("There should only be one edge", 1, paramToA.size());
		DataDependence paramToAExpected= new DataDependence(param, produceA, listType, "[param]");
		assertTrue("The dependency edge param -> a is missing", paramToA.contains(paramToAExpected));

		// Contains a self loop since we need to retrieve values from the parameter using get(0)
		Set<? extends DataDependence> aToA= pdg.getEdgeLabels(produceA, produceA);
		assertEquals("There should only be one edge", 2, aToA.size());
		// This means there is a dependency but it with an internal temp variable that we don't care about
		DataDependence aToAObject= new DataDependence(produceA, produceA, objectType, "[]");
		DataDependence aToAInteger= new DataDependence(produceA, produceA, integerType, "[]");
		assertTrue("The dependency edge a -> b is missing", aToA.contains(aToAObject));
		assertTrue("The dependency edge a -> b is missing", aToA.contains(aToAInteger));

		Set<? extends DataDependence> aToB= pdg.getEdgeLabels(produceA, produceB);
		assertEquals("There should only be one edge", 1, aToB.size());
		DataDependence aToBExpected= new DataDependence(produceA, produceB, primitiveInttype, "[a]");
		assertTrue("The dependency edge a -> b is missing", aToB.contains(aToBExpected));

		Set<? extends DataDependence> bToC= pdg.getEdgeLabels(produceB, produceC);
		assertEquals("There should only be one edge", 1, bToC.size());
		DataDependence bToCExpected= new DataDependence(produceB, produceC, primitiveInttype, "[b]");
		assertTrue("The dependency edge b -> c is missing", bToC.contains(bToCExpected));

	}

	/**
	 * Tests dependencies to a heap object.
	 * 
	 * TODO: This is the current behavior but we might want to change how this works to handle
	 * forwarding.
	 */
	@Test
	public void testProject7() throws IllegalArgumentException, IOException, CancelException, InvalidClassFileException {
		IR ir= retrieveMethodIR(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
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

		TypeReference intType= TypeReference.Int;
		TypeReference arrayListType= TypeReference.JavaUtilArrayList;

		Set<? extends DataDependence> createListToAddA= pdg.getEdgeLabels(createList, addA);
		assertEquals("There should only be one edge", 1, createListToAddA.size());
		DataDependence createListToAddAExpected= new DataDependence(createList, addA, arrayListType, "[list]");
		assertTrue("The dependency edge createList -> addA is missing", createListToAddA.contains(createListToAddAExpected));

		Set<? extends DataDependence> createListToAddB= pdg.getEdgeLabels(createList, addB);
		assertEquals("There should only be one edge", 1, createListToAddB.size());
		DataDependence createListToAddBExpected= new DataDependence(createList, addB, arrayListType, "[list]");
		assertTrue("The dependency edge createList -> addB is missing", createListToAddB.contains(createListToAddBExpected));

		Set<? extends DataDependence> aToB= pdg.getEdgeLabels(produceA, produceB);
		assertEquals("There should only be one edge", 1, aToB.size());
		DataDependence aToBExpected= new DataDependence(produceA, produceB, intType, "[a]");
		assertTrue("The dependency edge a -> b is missing", aToB.contains(aToBExpected));

		Set<? extends DataDependence> aToAddA= pdg.getEdgeLabels(produceA, addA);
		assertEquals("There should only be one edge", 1, aToAddA.size());
		DataDependence aToAddAExpected= new DataDependence(produceA, addA, intType, "[a]");
		assertTrue("The dependency edge a -> addA is missing", aToAddA.contains(aToAddAExpected));

		Set<? extends DataDependence> bToAddB= pdg.getEdgeLabels(produceB, addB);
		assertEquals("There should only be one edge", 1, bToAddB.size());
		DataDependence bToAddBExpected= new DataDependence(produceB, addB, intType, "[b]");
		assertTrue("The dependency edge b -> addB is missing", bToAddB.contains(bToAddBExpected));

		// Should have no edges
		assertTrue(pdg.getPredNodeCount(param) == 0);
		assertTrue(pdg.getSuccNodeCount(param) == 0);
	}
}
