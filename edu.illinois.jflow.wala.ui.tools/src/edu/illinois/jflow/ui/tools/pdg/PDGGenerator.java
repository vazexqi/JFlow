package edu.illinois.jflow.ui.tools.pdg;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

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
public class PDGGenerator {

	public static ProgramDependenceGraph makePDGForSelectedMethodInEditor(JavaEditor javaEditor, ICompilationUnit inputAsCompilationUnit, IJavaProject javaProject) throws IOException, CoreException,
			InvalidClassFileException {
		AbstractAnalysisEngine engine= new EclipseProjectAnalysisEngine(javaProject);
		engine.buildAnalysisScope();
		IClassHierarchy classHierarchy= engine.buildClassHierarchy();
		AnalysisOptions options= new AnalysisOptions();
		AnalysisCache cache= engine.makeDefaultCache();

		MethodReference method= JavaEditorUtil.findSelectedMethodDeclaration(javaEditor, inputAsCompilationUnit, classHierarchy);

		if (method != null) {
			IMethod resolvedMethod= classHierarchy.resolveMethod(method);
			if (resolvedMethod != null) {

				return buildPDGFromIMethod(options, cache, resolvedMethod);
			}
		}
		return null;
	}

	public static ProgramDependenceGraph buildPDGFromIMethod(AnalysisOptions options, AnalysisCache cache, IMethod resolvedMethod) throws InvalidClassFileException {
		IR ir= cache.getSSACache().findOrCreateIR(resolvedMethod, Everywhere.EVERYWHERE, options.getSSAOptions());
		ProgramDependenceGraph graph= ProgramDependenceGraph.make(ir);
		return graph;
	}
}
