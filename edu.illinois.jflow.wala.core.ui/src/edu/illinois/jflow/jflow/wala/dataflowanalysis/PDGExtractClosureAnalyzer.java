package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jface.text.IDocument;

import edu.illinois.jflow.source.utils.BindingsFinder;

/**
 * Delegates to PipelineStage to do the analysis internally with Wala IR. This is mostly a wrapper
 * for compatability between JDT and WALA. It has a way to return IBindings which is what the
 * refactoring operates on.
 * 
 * @author nchen
 * 
 */
public class PDGExtractClosureAnalyzer {
	private static final String THIS_PARAMETER= "this";

	private PipelineStage stage;

	public PDGExtractClosureAnalyzer(ProgramDependenceGraph pdg, IDocument doc, List<Integer> selectedLines) {
		stage= new PipelineStage(pdg, selectedLines);
	}

	/*
	 * This is mainly for testing purposes where we can test the line selections without the IDocument (UI-based)
	 */
	public PDGExtractClosureAnalyzer(ProgramDependenceGraph pdg, List<Integer> selectedLines) {
		stage= new PipelineStage(pdg, selectedLines);
	}

	/**
	 * The method that does the bulk of the analysis. This method will calculate all the input and
	 * output dependencies for the selected lines and the "outside" world.
	 */
	public void analyzeSelection() {
		stage.analyzeSelection();
	}

	public List<DataDependence> getInputDataDependences() {
		return stage.getInputDataDependences();
	}

	public List<DataDependence> getOutputDataDependences() {
		return stage.getOutputDataDependences();
	}

	public Set<String> getClosureLocalVariableNames() {
		return stage.getClosureLocalVariableNames();
	}

	public List<IVariableBinding> getLocalVariableBindings(ASTNode[] selectedNodes) {
		Map<String, IBinding> bindingsMap= transformNamesToBindings(selectedNodes, getClosureLocalVariableNames());
		return convertIBindingToIVariableBindingList(bindingsMap.values());
	}

	public List<IVariableBinding> getOutputBindings(ASTNode[] selectedNodes) {
		Map<String, IBinding> bindingsMap= transformDataDependencesToIBindings(selectedNodes, getOutputDataDependences());
		return convertIBindingToIVariableBindingList(bindingsMap.values());
	}

	public List<IVariableBinding> getInputBindings(ASTNode[] selectedNodes) {
		Map<String, IBinding> bindingsMap= transformDataDependencesToIBindings(selectedNodes, getInputDataDependences());
		return convertIBindingToIVariableBindingList(bindingsMap.values());
	}

	private List<IVariableBinding> convertIBindingToIVariableBindingList(Collection<IBinding> values) {
		List<IVariableBinding> bindings= new ArrayList<IVariableBinding>();
		for (IBinding binding : values) {
			bindings.add((IVariableBinding)binding);
		}
		return bindings;
	}

	// These methods are mostly for testing where we want to use the map to retrieve the particular binding
	// for a variable name. It is often easier to just iterate through the bindings using the methods above
	// e.g., getLocalVariableBindings, getOutputBindings, getInputBindings.
	public Map<String, IBinding> transformNamesToBindings(ASTNode[] nodes, Set<String> names) {
		return BindingsFinder.findBindings(nodes, names);
	}

	public Map<String, IBinding> transformDataDependencesToIBindings(ASTNode[] nodes, Collection<DataDependence> dependencies) {
		Set<String> names= extractNamesFromDependencies(dependencies);
		names.remove(THIS_PARAMETER); // Filter out the "this" parameter.
		return transformNamesToBindings(nodes, names);
	}

	private Set<String> extractNamesFromDependencies(Collection<DataDependence> dependencies) {
		Set<String> names= new HashSet<String>();

		for (DataDependence dependence : dependencies) {
			names.addAll(dependence.getLocalVariableNames());
		}

		return names;
	}
}
