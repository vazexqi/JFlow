package edu.illinois.jflow.wala.ui.tools.callgraph;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.Action;

import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;

import edu.illinois.jflow.wala.ui.tools.graph.jdt.util.JavaEditorUtil;
import edu.illinois.jflow.wala.ui.tools.graph.view.WalaGraphView;
import edu.illinois.jflow.wala.utils.EclipseProjectAnalysisEngine;

@SuppressWarnings("restriction")
public class GenerateCallGraph extends Action {

	private final WalaGraphView view;

	public GenerateCallGraph(WalaGraphView view) {
		this.view= view;
	}

	@Override
	public void run() {
		JavaEditor javaEditor= JavaEditorUtil.getActiveJavaEditor();

		if (javaEditor != null) {
			ICompilationUnit inputAsCompilationUnit= SelectionConverter.getInputAsCompilationUnit(javaEditor);
			IJavaProject javaProject= inputAsCompilationUnit.getJavaProject();
			try {
				AbstractAnalysisEngine engine= new EclipseProjectAnalysisEngine(javaProject);
				Graph<CGNode> callGraph= engine.buildDefaultCallGraph();
				Graph<CGNode> prunedGraph= GraphSlicer.prune(callGraph, new Predicate<CGNode>() {

					@Override
					public boolean test(CGNode node) {
						return node.getMethod().getDeclaringClass().getClassLoader() instanceof JavaSourceLoaderImpl;
					}
				});
				view.updateGraph(prunedGraph);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (CancelException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

}
