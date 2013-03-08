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

import com.ibm.wala.cast.java.client.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.ipa.callgraph.AstJavaZeroOneContainerCFABuilder;
import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.ide.util.EclipseFileProvider;
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
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.BypassClassTargetSelector;
import com.ibm.wala.ipa.summaries.BypassMethodTargetSelector;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.strings.Atom;

import edu.illinois.jflow.wala.core.Activator;
import edu.illinois.jflow.wala.pointeranalysis.AnalysisUtils;
import edu.illinois.jflow.wala.pointeranalysis.KObjectSensitiveContextSelector;

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
	static AstJavaZeroOneContainerCFABuilder getCallGraphBuilder(AnalysisScope scope, IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
		ContextSelector contextSelector= new KObjectSensitiveContextSelector();

		Util.addDefaultSelectors(options, cha);
		Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);
		addCustomBypassLogic(scope, cha, options);

		return new AstJavaZeroOneContainerCFABuilder(cha, options, cache, contextSelector, null) {

			@Override
			protected ZeroXInstanceKeys makeInstanceKeys(IClassHierarchy cha, AnalysisOptions options, SSAContextInterpreter contextInterpreter) {
				// Do not smush primitive holders – we do want to distinguish primitive holders
				ZeroXInstanceKeys zik= new ZeroXInstanceKeys(options, cha, contextInterpreter, ZeroXInstanceKeys.ALLOCATIONS | ZeroXInstanceKeys.SMUSH_STRINGS | ZeroXInstanceKeys.SMUSH_MANY
						| ZeroXInstanceKeys.SMUSH_THROWABLES);
				return zik;
	}

		};
	}

	/*
	 * See com.ibm.wala.ipa.callgraph.impl.Util.addBypassLogic(AnalysisOptions, AnalysisScope, ClassLoader, String, IClassHierarchy)
	 */
	static void addCustomBypassLogic(AnalysisScope analysisScope, IClassHierarchy classHierarchy, AnalysisOptions analysisOptions) throws IllegalArgumentException {
		ClassLoader classLoader= Util.class.getClassLoader();
		if (classLoader == null) {
			throw new IllegalArgumentException("classLoader is null");
		}

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
