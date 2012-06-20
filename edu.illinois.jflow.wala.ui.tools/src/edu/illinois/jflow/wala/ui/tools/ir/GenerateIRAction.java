package edu.illinois.jflow.wala.ui.tools.ir;

import java.io.IOException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;

import com.ibm.wala.cast.java.translator.jdt.JDTIdentityMapper;
import com.ibm.wala.cfg.CFGSanitizer;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAOptions;
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

				MethodReference method= findSelectedMethodDeclaration(javaEditor, inputAsCompilationUnit, classHierarchy);
				if (method != null) {
					IMethod resolvedMethod= classHierarchy.resolveMethod(method);
					if (resolvedMethod != null) {
						AnalysisOptions options= new AnalysisOptions();
						options.getSSAOptions().setPiNodePolicy(SSAOptions.getAllBuiltInPiNodes());
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

	private MethodReference findSelectedMethodDeclaration(JavaEditor javaEditor, ICompilationUnit inputAsCompilationUnit, IClassHierarchy classHierarchy) {
		ITextSelection selection= (ITextSelection)javaEditor.getSelectionProvider().getSelection();

		// 1) Get the ast for the editor
		CompilationUnit ast= SharedASTProvider.getAST(inputAsCompilationUnit, SharedASTProvider.WAIT_ACTIVE_ONLY, new NullProgressMonitor());

		// 2) Find the selected node
		NodeFinder nodeFinder= new NodeFinder(ast, selection.getOffset(), selection.getLength());
		ASTNode coveringNode= nodeFinder.getCoveringNode();

		// 3) If we have the right selection, convert it to a MethodReference
		if (coveringNode instanceof SimpleName && coveringNode.getParent() instanceof MethodDeclaration) {
			MethodDeclaration methodDeclaration= (MethodDeclaration)coveringNode.getParent();
			if (methodDeclaration.getName() == coveringNode) {
				IClassLoader[] loaders= classHierarchy.getLoaders();
				IClassLoader lastLoader= loaders[loaders.length - 1];
				JDTIdentityMapper mapper= new JDTIdentityMapper(lastLoader.getReference(), ast.getAST());
				return mapper.getMethodRef(methodDeclaration.resolveBinding());
			}
		}

		return null;
	}
}
