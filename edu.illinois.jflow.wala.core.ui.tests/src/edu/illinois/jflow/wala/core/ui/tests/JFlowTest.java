package edu.illinois.jflow.wala.core.ui.tests;



import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;

import com.ibm.wala.cast.java.client.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.test.JDTJavaTest;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ide.tests.util.EclipseTestUtil.ZippedProjectData;
import com.ibm.wala.ide.util.EclipseFileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;

import edu.illinois.jflow.wala.utils.JFlowAnalysisUtil;

public abstract class JFlowTest extends JDTJavaTest {

	protected AbstractAnalysisEngine engine;

	protected CallGraph callGraph;

	abstract String getTestPackageName();

	public JFlowTest(ZippedProjectData project) {
		super(project);
	}

	protected String constructFullyQualifiedClass() {
		return getTestPackageName() + "/" + singleInputForTest();
	}

	@Override
	protected AbstractAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors, List<String> libs) {
		return makeAnalysisEngine(mainClassDescriptors, libs, projectName);
	}

	static AbstractAnalysisEngine makeAnalysisEngine(final String[] mainClassDescriptors, List<String> libs, String projectName) {
		AbstractAnalysisEngine engine;
		try {
			engine= new JDTJavaSourceAnalysisEngine(projectName) {
				@Override
				protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
					return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE, cha, mainClassDescriptors);
				}

				@Override
				protected CallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
					return JFlowAnalysisUtil.getCallGraphBuilder(scope, cha, options, cache);
				}
			};

			try {
				engine.setExclusionsFile((new EclipseFileProvider()).getFileFromPlugin(Activator.getDefault(), "Java60RegressionExclusions.txt").getAbsolutePath());
			} catch (IOException e) {
				Assert.assertFalse("Cannot find exclusions file", true);
			}

			return engine;
		} catch (IOException e1) {
			Assert.fail(e1.getMessage());
			return null;
		} catch (CoreException e1) {
			Assert.fail(e1.getMessage());
			return null;
		}
	}

	protected IR retrieveMethodToBeInspected(String fullyQualifiedClassName, String methodName, String methodParameters, String returnType) throws IOException, IllegalArgumentException,
			CancelException {
		IMethod method= retrieveMethod(fullyQualifiedClassName, methodName, methodParameters, returnType);
		CGNode node= callGraph.getNode(method, Everywhere.EVERYWHERE);
		return engine.getCache().getIR(method);
	}

	protected IMethod retrieveMethod(String fullyQualifiedClassName, String methodName, String methodParameters, String returnType) throws IOException, IllegalArgumentException, CancelException {
		engine= getAnalysisEngine(simplePkgTestEntryPoint(getTestPackageName()), rtJar);

		callGraph= engine.buildDefaultCallGraph();

		IClassHierarchy classHierarchy= engine.getClassHierarchy();

		MethodReference methodRef= descriptorToMethodRef(String.format("Source#%s#%s#(%s)%s", fullyQualifiedClassName, methodName, methodParameters, returnType), classHierarchy);
		IMethod method= classHierarchy.resolveMethod(methodRef);
		return method;
	}

}
