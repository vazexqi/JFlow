package edu.illinois.jflow.ui.tools.pdg;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;

import edu.illinois.jflow.jflow.wala.dataflowanalysis.ProgramDependenceGraph;
import edu.illinois.jflow.wala.ui.tools.graph.jdt.util.JavaEditorUtil;
import edu.illinois.jflow.wala.utils.EclipseProjectAnalysisEngine;

@SuppressWarnings("restriction")
public class GeneratePDGAction extends Action {

	private final PDGView view;

	public GeneratePDGAction(PDGView view) {
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
				engine.buildAnalysisScope();
				IClassHierarchy classHierarchy= engine.buildClassHierarchy();

				MethodReference method= JavaEditorUtil.findSelectedMethodDeclaration(javaEditor, inputAsCompilationUnit, classHierarchy);
				if (method != null) {
					IMethod resolvedMethod= classHierarchy.resolveMethod(method);
					if (resolvedMethod != null) {
						AnalysisOptions options= new AnalysisOptions();
						AnalysisCache cache= engine.makeDefaultCache();

						try {
							IR ir= cache.getSSACache().findOrCreateIR(resolvedMethod, Everywhere.EVERYWHERE, options.getSSAOptions());
							ProgramDependenceGraph graph= ProgramDependenceGraph.make(ir);
							IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
							view.setDocument(document);
							view.setPDG(graph);
							view.updateGraph(graph);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (InvalidClassFileException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}

		}

	}


}
