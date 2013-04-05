package edu.illinois.jflow.wala.core.ui.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.junit.Test;

import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.ide.util.JdtUtil;
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

	private CompilationUnit getCompilationUnit() {
		IType type= getTypeForCurrentTestProject(JdtUtil.getJavaProject(PROJECT_NAME));
		if (type != null) {
			ICompilationUnit iCUnit= type.getCompilationUnit();
			if (iCUnit != null) {
				final ASTParser parser= ASTParser.newParser(AST.JLS4);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setSource(iCUnit);
				parser.setResolveBindings(true); // Must set to true since this is what we are testing
				CompilationUnit unit= (CompilationUnit)parser.createAST(new NullProgressMonitor());
				return unit;
			}
		}

		return null;
	}

	private IType getTypeForCurrentTestProject(IJavaProject javaProject) {
		return JdtUtil.findJavaClassInProjects(getFullyQualifiedNameForCurrentTestProject(), Arrays.asList(javaProject));
	}

	private String getFullyQualifiedNameForCurrentTestProject() {
		// JDT likes . separators whereas Wala likes / separators
		String walaQualifiedName= constructFullyQualifiedClass();
		String jdtQualifiedName= walaQualifiedName.replace('/', '.');
		return jdtQualifiedName;
	}

	private ASTNode getMethodDeclaration(CompilationUnit cUnit, String selector) {
		IType type= getTypeForCurrentTestProject(JdtUtil.getJavaProject(PROJECT_NAME));
		String name= JdtUtil.parseForName(selector, type);
		String[] parameters= JdtUtil.parseForParameterTypes(selector);
		IMethod method= type.getMethod(name, parameters);
		ASTNode methodNode= cUnit.findDeclaringNode(method.getKey());
		return methodNode;
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
			assertTrue("There should be one local variable [b] for the method", analyzer.getClosureLocalVariableNames().contains("b"));

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

			// Verify simple properties
			assertEquals("There should be 1 input dependencies", 1, analyzer.getInputDataDependences().size());
			assertEquals("There should be 1 output dependencies", 1, analyzer.getOutputDataDependences().size());
			assertTrue("There should be one local variable [b] for the method", analyzer.getClosureLocalVariableNames().contains("b"));

			// Verify structure
			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode produceC= pdg.getNode(3);

			DataDependence input= analyzer.getInputDataDependences().get(0);
			DataDependence expectedInput= new DataDependence(produceA, produceB, TypeReference.Int, "[a]");
			assertEquals("a->b input dependence doesn't match", expectedInput, input);

			DataDependence output= analyzer.getOutputDataDependences().get(0);
			DataDependence expectedOutput= new DataDependence(produceB, produceC, TypeReference.Int, "[b]");
			assertEquals("b->c input dependence doesn't match", expectedOutput, output);

			// Verify that we can reconcile with JDT with bindings
			CompilationUnit cUnit= getCompilationUnit();
			ASTNode methodNode= getMethodDeclaration(cUnit, "main([Ljava/lang/String;)");
			Map<String, IBinding> bindings= analyzer.transformNamesToBindings(new ASTNode[] { methodNode }, analyzer.getClosureLocalVariableNames());

			IBinding bindingForB= bindings.get("b");
			assertTrue("Expected a binding for variable b but got null instead", bindingForB != null);
			assertEquals("The key for the binding doesn't match", "Lanalyzer/Project2;.main([Ljava/lang/String;)V#b", bindingForB.getKey());
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

			// Verify simple properties
			assertEquals("There should 2 input dependencies", 2, analyzer.getInputDataDependences().size());
			assertEquals("There should 1 output dependencies", 1, analyzer.getOutputDataDependences().size());
			assertTrue("There should be one local variable [b] for the method", analyzer.getClosureLocalVariableNames().contains("b"));

			// Verify structure
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

			// Verify that we can reconcile with JDT with bindings
			CompilationUnit cUnit= getCompilationUnit();
			ASTNode methodNode= getMethodDeclaration(cUnit, "main([Ljava/lang/String;)");
			Map<String, IBinding> bindings= analyzer.transformNamesToBindings(new ASTNode[] { methodNode }, analyzer.getClosureLocalVariableNames());

			IBinding bindingForB= bindings.get("b");
			assertTrue("Expected a binding for variable b but got null instead", bindingForB != null);
			assertEquals("The key for the binding doesn't match", "Lanalyzer/Project3;.main([Ljava/lang/String;)V#b", bindingForB.getKey());
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

			// Verify simple properties
			assertEquals("There should 2 input dependencies", 2, analyzer.getInputDataDependences().size());
			assertEquals("There should 1 output dependencies", 1, analyzer.getOutputDataDependences().size());
			assertTrue("There should be one local variable [b] for the method", analyzer.getClosureLocalVariableNames().contains("b"));

			// Verify structure
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

			// Verify that we can reconcile with JDT with bindings
			CompilationUnit cUnit= getCompilationUnit();
			ASTNode methodNode= getMethodDeclaration(cUnit, "main([Ljava/lang/String;)");
			Map<String, IBinding> bindings= analyzer.transformNamesToBindings(new ASTNode[] { methodNode }, analyzer.getClosureLocalVariableNames());

			IBinding bindingForB= bindings.get("b");
			assertTrue("Expected a binding for variable b but got null instead", bindingForB != null);
			assertEquals("The key for the binding doesn't match", "Lanalyzer/Project4;.main([Ljava/lang/String;)V#b", bindingForB.getKey());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testProject5() {
		try {
			IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
			List<Integer> lines= Arrays.asList(10, 11);
			PDGExtractClosureAnalyzer analyzer= new PDGExtractClosureAnalyzer(pdg, lines);
			analyzer.analyzeSelection();

			// Verify simple properties
			assertEquals("There should 2 input dependencies", 2, analyzer.getInputDataDependences().size());
			assertEquals("There should 2 output dependencies", 2, analyzer.getOutputDataDependences().size());
			assertTrue("There should be one local variable [b] for the method", analyzer.getClosureLocalVariableNames().contains("b"));

			// Verify structure
			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode plusEqualsA= pdg.getNode(3);
			PDGNode consumeC= pdg.getNode(4);

			// This one is tricky and I want to check that it is right. 
			// Basically all the dependencies are on 'a' but 'a' has been modified several times
			DataDependence input1= analyzer.getInputDataDependences().get(0);
			DataDependence expectedInput1= new DataDependence(produceA, produceB, TypeReference.Int, "[a]");
			assertEquals("a->b input dependence doesn't match", expectedInput1, input1);

			DataDependence input2= analyzer.getInputDataDependences().get(1);
			DataDependence expectedInput2= new DataDependence(produceA, plusEqualsA, TypeReference.Int, "[a]");
			assertEquals("a->a+= input dependence doesn't match", expectedInput2, input2);

			DataDependence output1= analyzer.getOutputDataDependences().get(0);
			DataDependence expectedOutput1= new DataDependence(produceB, consumeC, TypeReference.Int, "[b]");
			assertEquals("b -> c input dependence doesn't match", expectedOutput1, output1);

			DataDependence output2= analyzer.getOutputDataDependences().get(1);
			DataDependence expectedOutput2= new DataDependence(plusEqualsA, consumeC, TypeReference.Int, "[a]");
			assertEquals("a+=->c input dependence doesn't match", expectedOutput2, output2);

			// Verify that we can reconcile with JDT with bindings
			CompilationUnit cUnit= getCompilationUnit();
			ASTNode methodNode= getMethodDeclaration(cUnit, "main([Ljava/lang/String;)");
			Map<String, IBinding> bindings= analyzer.transformNamesToBindings(new ASTNode[] { methodNode }, analyzer.getClosureLocalVariableNames());

			IBinding bindingForB= bindings.get("b");
			assertTrue("Expected a binding for variable b but got null instead", bindingForB != null);
			assertEquals("The key for the binding doesn't match", "Lanalyzer/Project5;.main([Ljava/lang/String;)V#b", bindingForB.getKey());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testProject6() {
		try {
			IR ir= retrieveMethodToBeInspected(constructFullyQualifiedClass(), "main", "[Ljava/lang/String;", "V");
			ProgramDependenceGraph pdg= ProgramDependenceGraph.make(ir, engine.buildClassHierarchy());
			List<Integer> lines= Arrays.asList(10, 11, 12);
			PDGExtractClosureAnalyzer analyzer= new PDGExtractClosureAnalyzer(pdg, lines);
			analyzer.analyzeSelection();

			// Verify simple properties
			assertEquals("There should 2 input dependencies", 2, analyzer.getInputDataDependences().size());
			assertEquals("There should 0 output dependencies", 0, analyzer.getOutputDataDependences().size());
			assertTrue("There should be two local variable [b,c] for the method", analyzer.getClosureLocalVariableNames().containsAll(new HashSet<String>(Arrays.asList("b", "c"))));

			// Verify structure
			PDGNode produceA= pdg.getNode(1);
			PDGNode produceB= pdg.getNode(2);
			PDGNode plusEqualsA= pdg.getNode(3);

			// This one is tricky and I want to check that it is right. 
			// Basically all the dependencies are on 'a' but 'a' has been modified several times
			DataDependence input1= analyzer.getInputDataDependences().get(0);
			DataDependence expectedInput1= new DataDependence(produceA, produceB, TypeReference.Int, "[a]");
			assertEquals("a->b input dependence doesn't match", expectedInput1, input1);

			DataDependence input2= analyzer.getInputDataDependences().get(1);
			DataDependence expectedInput2= new DataDependence(produceA, plusEqualsA, TypeReference.Int, "[a]");
			assertEquals("a->a+= input dependence doesn't match", expectedInput2, input2);

			// Verify that we can reconcile with JDT with bindings
			CompilationUnit cUnit= getCompilationUnit();
			ASTNode methodNode= getMethodDeclaration(cUnit, "main([Ljava/lang/String;)");
			Map<String, IBinding> bindings= analyzer.transformNamesToBindings(new ASTNode[] { methodNode }, analyzer.getClosureLocalVariableNames());

			IBinding bindingForB= bindings.get("b");
			assertTrue("Expected a binding for variable b but got null instead", bindingForB != null);
			assertEquals("The key for the binding doesn't match", "Lanalyzer/Project6;.main([Ljava/lang/String;)V#b", bindingForB.getKey());

			IBinding bindingForC= bindings.get("c");
			assertTrue("Expected a binding for variable c but got null instead", bindingForC != null);
			assertEquals("The key for the binding doesn't match", "Lanalyzer/Project6;.main([Ljava/lang/String;)V#c", bindingForC.getKey());

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			e.printStackTrace();
		}
	}
}
