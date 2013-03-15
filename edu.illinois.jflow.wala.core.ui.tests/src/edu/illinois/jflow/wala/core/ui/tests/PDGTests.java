package edu.illinois.jflow.wala.core.ui.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

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

	public PDGTests() {
		super(new ZippedProjectData(Activator.getDefault(), PROJECT_NAME, PROJECT_ZIP));
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
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir);

			// Verify
			assertEquals("Number of nodes not expected", 2, pdg.getNumberOfNodes());
			for (PDGNode pdgNode : Iterator2Iterable.make(pdg.iterator())) {
				// Should have no edges
				assertTrue(pdg.getPredNodeCount(pdgNode) == 0);
				assertTrue(pdg.getSuccNodeCount(pdgNode) == 0);
			}


		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}

	private String constructFullyQualifiedClass() {
		return TEST_PACKAGE_NAME + "/" + singleInputForTest();
	}

	private IR retrieveMethodToBeInspected(String fullyQualifiedClassName, String methodName, String methodParameters, String returnType) throws IOException {
		AbstractAnalysisEngine engine= getAnalysisEngine(simplePkgTestEntryPoint(TEST_PACKAGE_NAME), rtJar);
		engine.buildAnalysisScope();
		IClassHierarchy classHierarchy= engine.buildClassHierarchy();

		MethodReference methodRef= descriptorToMethodRef(String.format("Source#%s#%s#(%s)%s", fullyQualifiedClassName, methodName, methodParameters, returnType), classHierarchy);
		IMethod method= classHierarchy.resolveMethod(methodRef);
		return engine.getCache().getSSACache().findOrCreateIR(method, Everywhere.EVERYWHERE, new AnalysisOptions().getSSAOptions());
	}

}
