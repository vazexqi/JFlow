package edu.illinois.jflow.wala.core.ui.tests;



import java.io.IOException;

import com.ibm.wala.cast.java.test.JDTJavaTest;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;

public abstract class JFlowTest extends JDTJavaTest {

	protected AbstractAnalysisEngine engine;

	abstract String getTestPackageName();

	public JFlowTest(ZippedProjectData project) {
		super(project);
	}

	protected String constructFullyQualifiedClass() {
		return getTestPackageName() + "/" + singleInputForTest();
	}

	protected IR retrieveMethodToBeInspected(String fullyQualifiedClassName, String methodName, String methodParameters, String returnType) throws IOException {
		engine= getAnalysisEngine(simplePkgTestEntryPoint(getTestPackageName()), rtJar);
		engine.buildAnalysisScope();
		IClassHierarchy classHierarchy= engine.buildClassHierarchy();

		MethodReference methodRef= descriptorToMethodRef(String.format("Source#%s#%s#(%s)%s", fullyQualifiedClassName, methodName, methodParameters, returnType), classHierarchy);
		IMethod method= classHierarchy.resolveMethod(methodRef);
		return engine.getCache().getIR(method);
	}

}
