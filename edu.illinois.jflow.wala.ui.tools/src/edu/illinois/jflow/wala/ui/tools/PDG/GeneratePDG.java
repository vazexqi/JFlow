package edu.illinois.jflow.wala.ui.tools.pdg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.Action;

import com.ibm.wala.cast.ipa.modref.AstModRef;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.slicer.MethodEntryStatement;
import com.ibm.wala.ipa.slicer.MethodExitStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;

import edu.illinois.jflow.wala.ui.tools.graph.jdt.util.JavaEditorUtil;
import edu.illinois.jflow.wala.ui.tools.graph.view.WalaGraphView;
import edu.illinois.jflow.wala.utils.EclipseProjectAnalysisEngine;

@SuppressWarnings("restriction")
public class GeneratePDG extends Action {

	private final WalaGraphView view;

	public GeneratePDG(WalaGraphView view) {
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
				CallGraphBuilder builder= engine.defaultCallGraphBuilder();
				CallGraph callGraph= engine.buildDefaultCallGraph();
				MethodReference method= JavaEditorUtil.findSelectedMethodDeclaration(javaEditor, inputAsCompilationUnit, engine.getClassHierarchy());
				if (method != null) {
					final SDG sdg= new SDG(callGraph, builder.getPointerAnalysis(), new AstJavaModRef(), DataDependenceOptions.NO_EXCEPTIONS, ControlDependenceOptions.NONE);
					Set<CGNode> nodes= callGraph.getNodes(method);
					if (!nodes.isEmpty()) {
						// Take the first one for now
						List<CGNode> list= new ArrayList<CGNode>(nodes);
						CGNode node= list.get(0);
						PDG pdg= sdg.getPDG(node);
						System.err.println("Unpruned PDG number of nodes: " + pdg.getNumberOfNodes());
						Graph<Statement> prunedPDG= GraphSlicer.prune(pdg, new Predicate<Statement>() {

							@Override
							public boolean test(Statement node) {
								if (node.getNode().getMethod().getDeclaringClass().getClassLoader() instanceof JavaSourceLoaderImpl) {
									if (node.getNode().equals(sdg.getCallGraph().getFakeRootNode())) {
										return false;
									} else if (node instanceof MethodExitStatement || node instanceof MethodEntryStatement) {
										return false;
									} else {
										return true;
									}
								}
								return false;
							}
						});
						System.err.println("Pruned PDG number of nodes: " + prunedPDG.getNumberOfNodes());
						view.updateGraph(prunedPDG);
					}
				}
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
