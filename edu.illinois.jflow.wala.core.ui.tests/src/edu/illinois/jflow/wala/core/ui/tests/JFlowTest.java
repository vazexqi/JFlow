package edu.illinois.jflow.wala.core.ui.tests;



import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.ibm.wala.cast.java.client.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.cast.java.test.JDTJavaTest;
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
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.viz.viewer.WalaViewer;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.PipelineStage;
import edu.illinois.jflow.wala.utils.JFlowAnalysisUtil;

public abstract class JFlowTest extends JDTJavaTest {

	protected AbstractAnalysisEngine engine;

	protected CallGraph callGraph;

	protected abstract String getTestPackageName();

	public JFlowTest(ZippedProjectData project) {
		super(project);
	}

	protected String constructFullyQualifiedClass() {
		return getTestPackageName() + "/" + singleInputForTest();
	}

	protected String constructFullyQualifiedClass(String name) {
		return getTestPackageName() + "/" + name;
	}

	@Override
	protected AbstractAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors, Collection<String> sources,
			List<String> libs) {
		return makeAnalysisEngine(mainClassDescriptors, sources, libs, project);
	}

	static AbstractAnalysisEngine makeAnalysisEngine(final String[] mainClassDescriptors, Collection<String> sources, List<String> libs, ZippedProjectData project) {
		AbstractAnalysisEngine engine;
		try {
			engine= new JDTJavaSourceAnalysisEngine(project.projectName) {
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

	protected IR retrieveMethodIR(String fullyQualifiedClassName, String methodName, String methodParameters, String returnType) throws IOException, IllegalArgumentException,
			CancelException {
		MethodReference method= retrieveMethod(fullyQualifiedClassName, methodName, methodParameters, returnType);
		Set<CGNode> nodes= callGraph.getNodes(method);

		Assertions.productionAssertion(nodes.size() == 1, "Expected a single corresponding CGNode, but got either 0 or more");

		CGNode node= nodes.iterator().next(); // Quick way to get first element of set with single entry since set doesn't implement get();
		IR ir= node.getIR();
		return ir;
	}

	protected MethodReference retrieveMethod(String fullyQualifiedClassName, String methodName, String methodParameters, String returnType) throws IOException, IllegalArgumentException,
			CancelException {
		engine= getAnalysisEngine(simplePkgTestEntryPoint(getTestPackageName()), null, rtJar);

		callGraph= engine.buildDefaultCallGraph();

		IClassHierarchy classHierarchy= engine.getClassHierarchy();

		MethodReference methodRef= descriptorToMethodRef(String.format("Source#%s#%s#(%s)%s", fullyQualifiedClassName, methodName, methodParameters, returnType), classHierarchy);
		return methodRef;
	}

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

	protected void openShell() {
		Shell shell= new Shell(Display.getDefault());
		(new Dialog(shell) {
			public void open() {
				Shell parent= getParent();
				Shell shell= new Shell(parent, SWT.EMBEDDED | SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
				shell.setSize(600, 800);

				Frame frame= SWT_AWT.new_Frame(shell);
				frame.setSize(600, 800);
				frame.setLayout(new BorderLayout());
				frame.add(new WalaViewer(callGraph, engine.getPointerAnalysis()), BorderLayout.CENTER);
				frame.pack();
				frame.setVisible(true);

				shell.pack();
				shell.open();
				Display display= parent.getDisplay();
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch())
						display.sleep();
				}
			}
		}).open();
	}

	protected void printModRefInfo(PipelineStage stage) {
		System.err.println("<<<REF>>>");
		System.err.println(stage.getPrettyPrintRefs());

		System.err.println("<<<MOD>>>");
		System.err.println(stage.getPrettyPrintMods());

		System.err.println("<<IGNORED>>");
		System.err.println(stage.getPrettyPrintIgnored());
	}

}
