package edu.illinois.jflow.wala.ui.tools.pdg;

import java.io.IOException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.MethodExitStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;

import edu.illinois.jflow.wala.ui.tools.graph.view.WalaGraphView;
import edu.illinois.jflow.wala.utils.EclipseProjectAnalysisEngine;

@SuppressWarnings("restriction")
public class GenerateProgramDependenceGraph extends Action {

	private final WalaGraphView view;

	public GenerateProgramDependenceGraph(WalaGraphView view) {
		this.view= view;
	}

	@Override
	public void run() {
		IEditorPart activeEditor= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (activeEditor instanceof JavaEditor) {
			JavaEditor javaEditor= (JavaEditor)activeEditor;
			ICompilationUnit inputAsCompilationUnit= SelectionConverter.getInputAsCompilationUnit(javaEditor);
			IJavaProject javaProject= inputAsCompilationUnit.getJavaProject();
			AbstractAnalysisEngine engine= new EclipseProjectAnalysisEngine(javaProject);
			try {
				CallGraphBuilder builder= engine.defaultCallGraphBuilder();
				long t1= System.currentTimeMillis();
				CallGraph callGraph= engine.buildDefaultCallGraph();
				long t2= System.currentTimeMillis();
				System.out.println(t2 - t1);
				SDG sdg= new SDG(callGraph, builder.getPointerAnalysis(), DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS, ControlDependenceOptions.FULL);
				long t3= System.currentTimeMillis();
				System.out.println(t3 - t2);
				Graph<Statement> pruneSDG= pruneSDG(sdg);
				System.out.println("Number of nodes:" + pruneSDG.getNumberOfNodes());
				view.updateGraph(pruneSDG);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (CancelException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private Graph<Statement> pruneSDG(final SDG sdg) {
		Predicate<Statement> predicate= new Predicate<Statement>() {
			@Override
			public boolean test(Statement statement) {
				Statement s= statement;
				if (s.getNode().equals(sdg.getCallGraph().getFakeRootNode())) {
					return false;
				} else if (s instanceof MethodExitStatement || s instanceof MethodEntryStatement) {
					return false;
				} else {
					return true;
				}
			}
		};
		return GraphSlicer.prune(sdg, predicate);
	}

}
