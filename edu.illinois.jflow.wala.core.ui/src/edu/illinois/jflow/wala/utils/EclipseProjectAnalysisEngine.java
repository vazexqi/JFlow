/**
 * This class derives from https://github.com/reprogrammer/keshmesh/ and is licensed under Illinois Open Source License.
 */
package edu.illinois.jflow.wala.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ide.util.EclipseAnalysisScopeReader;
import com.ibm.wala.ide.util.EclipseFileProvider;
import com.ibm.wala.ide.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXContainerCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.BypassClassTargetSelector;
import com.ibm.wala.ipa.summaries.BypassMethodTargetSelector;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.strings.Atom;

import edu.illinois.jflow.wala.core.Activator;
import edu.illinois.jflow.wala.pointeranalysis.AnalysisUtils;
import edu.illinois.jflow.wala.pointeranalysis.KObjectSensitiveContextSelector;


/**
 * Modified from EclipseAnalysisEngine.java, originally from Keshmesh. Authored by Mohsen Vakilian
 * and Stas Negara. Modified by Nicholas Chen.
 * 
 */
public class EclipseProjectAnalysisEngine extends AbstractAnalysisEngine {
	protected final IJavaProject javaProject;

	public EclipseProjectAnalysisEngine(IJavaProject javaProject) {
		this.javaProject= javaProject;
	}

	//TODO: Might add this from a preference pane
	private String retrieveExclusionFile() throws IOException {
		return new EclipseFileProvider().getFileFromPlugin(Activator.getDefault(), "EclipseDefaultExclusions.txt").getAbsolutePath();
	}

	@Override
	public void buildAnalysisScope() throws IOException {
		try {
			setExclusionsFile(retrieveExclusionFile());
			EclipseProjectPath eclipseProject= EclipseProjectPath.make(javaProject);

			// It is important to include the SYNTHETIC_J2SE_MODEL because it contains some Java implementation of the basic
			// native methods. This allows WALA to reason about things like Threads and System.arraycopy().
			// https://groups.google.com/forum/?fromgroups#!searchin/wala-sourceforge-net/primordial/wala-sourceforge-net/_HLdzc29AZ8/e3j7vf5dIxUJ
			AnalysisScope analysisScope= EclipseAnalysisScopeReader.readJavaScopeFromPlugin(SYNTHETIC_J2SE_MODEL, new File(getExclusionsFile()), getClass().getClassLoader(),
					Activator.getDefault());

			scope= eclipseProject.toAnalysisScope(analysisScope);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected CallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
		return JFlowAnalysisUtil.getCallGraphBuilder(scope, cha, options, cache);
	}

	//TODO: Implement facility to change this
	@Override
	protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
		return super.makeDefaultEntrypoints(scope, cha);
	}

}

class JFlowAnalysisUtil {

	/**
	 * http://wala.sourceforge.net/wiki/index.php/UserGuide:PointerAnalysis
	 * 
	 * The ZeroOneContainerCFA policy (see Util.makeZeroOneContainerCFABuilder) extends the
	 * ZeroOneCFA policy with unlimited object-sensitivity for collection objects. For any
	 * allocation sites in collection objects, the allocation site is named by a tuple of allocation
	 * sites extending to the outermost enclosing collection allocation. This policy can be
	 * relatively expensive, but can be effective in disambiguating contents of standard collection
	 * classes.
	 */
	static CallGraphBuilder getCallGraphBuilder(AnalysisScope analysisScope, IClassHierarchy classHierarchy, AnalysisOptions analysisOptions, AnalysisCache analysisCache) {
		ContextSelector contextSelector= new KObjectSensitiveContextSelector();
		addCustomBypassLogic(analysisScope, classHierarchy, analysisOptions);
		return makeZeroOneCFAContainerBuilder(analysisScope, classHierarchy, analysisOptions, analysisCache, contextSelector, null);
	}

	static SSAPropagationCallGraphBuilder makeZeroOneCFAContainerBuilder(AnalysisScope scope, IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache,
			ContextSelector customSelector, SSAContextInterpreter customInterpreter) {

		return new ZeroXContainerCFABuilder(cha, options, cache, customSelector, customInterpreter, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_MANY
				| ZeroXInstanceKeys.SMUSH_PRIMITIVE_HOLDERS
				| ZeroXInstanceKeys.SMUSH_STRINGS | ZeroXInstanceKeys.SMUSH_THROWABLES);
	}

	/*
	 * See com.ibm.wala.ipa.callgraph.impl.Util.addBypassLogic(AnalysisOptions, AnalysisScope, ClassLoader, String, IClassHierarchy)
	 */
	static void addCustomBypassLogic(AnalysisScope analysisScope, IClassHierarchy classHierarchy, AnalysisOptions analysisOptions) throws IllegalArgumentException {
		ClassLoader classLoader= Util.class.getClassLoader();
		if (classLoader == null) {
			throw new IllegalArgumentException("classLoader is null");
		}

		Util.addDefaultSelectors(analysisOptions, classHierarchy);

		InputStream inputStream= classLoader.getResourceAsStream(Util.nativeSpec);
		XMLMethodSummaryReader methodSummaryReader= new XMLMethodSummaryReader(inputStream, analysisScope);

		MethodTargetSelector customMethodTargetSelector= getCustomBypassMethodTargetSelector(classHierarchy, analysisOptions, methodSummaryReader);
		analysisOptions.setSelector(customMethodTargetSelector);

		ClassTargetSelector customClassTargetSelector= new BypassClassTargetSelector(analysisOptions.getClassTargetSelector(), methodSummaryReader.getAllocatableClasses(), classHierarchy,
				classHierarchy.getLoader(analysisScope.getLoader(Atom.findOrCreateUnicodeAtom("Synthetic"))));
		analysisOptions.setSelector(customClassTargetSelector);
	}

	static BypassMethodTargetSelector getCustomBypassMethodTargetSelector(IClassHierarchy classHierarchy, AnalysisOptions analysisOptions, XMLMethodSummaryReader summary) {
		return new JFlowBypassMethodTargetSelector(analysisOptions.getMethodTargetSelector(), summary.getSummaries(), summary.getIgnoredPackages(), classHierarchy);
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
