package edu.illinois.jflow.wala.ui.tools.ir;

import java.io.IOException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;

import com.ibm.wala.cfg.CFGSanitizer;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.Graph;

import edu.illinois.jflow.wala.ui.tools.graph.jdt.util.JavaEditorUtil;
import edu.illinois.jflow.wala.utils.EclipseProjectAnalysisEngine;

@SuppressWarnings("restriction")
public class GenerateIRAction extends Action {

	private final IRView view;

	public GenerateIRAction(IRView view) {
		this.view= view;
	}

	@Override
	public void run() {
		JavaEditor javaEditor= JavaEditorUtil.getActiveJavaEditor();
		if (javaEditor != null) {
			ICompilationUnit inputAsCompilationUnit= SelectionConverter.getInputAsCompilationUnit(javaEditor);
			IJavaProject javaProject= inputAsCompilationUnit.getJavaProject();
			AbstractAnalysisEngine engine= new EclipseProjectAnalysisEngine(javaProject);
			try {
				engine.buildAnalysisScope();
				IClassHierarchy classHierarchy= engine.buildClassHierarchy();

				MethodReference method= JavaEditorUtil.findSelectedMethodDeclaration(javaEditor, inputAsCompilationUnit, classHierarchy);
				if (method != null) {
					IMethod resolvedMethod= classHierarchy.resolveMethod(method);
					if (resolvedMethod != null) {
						AnalysisOptions options= new AnalysisOptions();
						AnalysisCache cache= new AnalysisCache();

						try {
							IR ir= cache.getSSACache().findOrCreateIR(resolvedMethod, Everywhere.EVERYWHERE, options.getSSAOptions());
							Graph<? extends ISSABasicBlock> graph= ir.getControlFlowGraph();
							graph= CFGSanitizer.sanitize(ir, classHierarchy);
							IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
							view.setIR(ir);
							view.setDocument(document);
							view.updateGraph(graph);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (WalaException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}
}
