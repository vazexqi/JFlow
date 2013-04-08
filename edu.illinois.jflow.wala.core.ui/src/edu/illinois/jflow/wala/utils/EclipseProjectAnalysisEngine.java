/**
 * This class derives from https://github.com/reprogrammer/keshmesh/ and is licensed under Illinois Open Source License.
 */
package edu.illinois.jflow.wala.utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.cast.java.client.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.ide.util.EclipseFileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.BypassMethodTargetSelector;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.strings.Atom;

import edu.illinois.jflow.wala.core.Activator;
import edu.illinois.jflow.wala.pointeranalysis.AnalysisUtils;

/**
 * Modified from EclipseAnalysisEngine.java, originally from Keshmesh. Authored by Mohsen Vakilian
 * and Stas Negara. Modified by Nicholas Chen.
 * 
 */
public class EclipseProjectAnalysisEngine extends JDTJavaSourceAnalysisEngine {

	public EclipseProjectAnalysisEngine(IJavaProject project) throws IOException, CoreException {
		super(project);
	}

	private String retrieveExclusionFile() throws IOException {
		return new EclipseFileProvider().getFileFromPlugin(Activator.getDefault(), "EclipseDefaultExclusions.txt").getAbsolutePath();
	}

	@Override
	public void buildAnalysisScope() throws IOException {
		super.scope= ePath.toAnalysisScope(makeAnalysisScope());
		if (getExclusionsFile() != null) {
			scope.setExclusions(FileOfClasses.createFileOfClasses(new File(retrieveExclusionFile())));
		}
	}

	@Override
	protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
		return Util.makeMainEntrypoints(JavaSourceAnalysisScope.SOURCE, cha);
	}

	@Override
	protected CallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
		return (CallGraphBuilder)JFlowAnalysisUtil.getCallGraphBuilder(scope, cha, options, cache);
	}

}

class JFlowBypassMethodTargetSelector extends BypassMethodTargetSelector {

	public JFlowBypassMethodTargetSelector(MethodTargetSelector parent, Map<MethodReference, MethodSummary> methodSummaries, Set<Atom> ignoredPackages, IClassHierarchy cha) {
		super(parent, methodSummaries, ignoredPackages, cha);
	}

	@Override
	protected boolean canIgnore(MemberReference m) {
		if (AnalysisUtils.isLibraryClass(m.getDeclaringClass())) {
			return true;
		} else {
			return super.canIgnore(m);
		}
	}
}
