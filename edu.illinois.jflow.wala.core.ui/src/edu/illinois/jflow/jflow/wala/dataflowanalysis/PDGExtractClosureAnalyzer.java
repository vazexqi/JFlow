package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.ibm.wala.util.collections.Iterator2Iterable;

import edu.illinois.jflow.source.utils.BindingsFinder;

public class PDGExtractClosureAnalyzer {
	private ProgramDependenceGraph pdg;

	private List<Integer> selectedLines; // This is 1-based following how things "look" in the editor

	private List<PDGNode> selectedStatements= new ArrayList<PDGNode>(); // The actual selected statements (PDGNodes) in the editor

	private List<DataDependence> inputDataDependences; // Think of these as method parameters

	private List<DataDependence> outputDataDependences; // Think of these as method return value(S) <-- yes possibly values!

	private Set<String> closureLocalVariableNames;

	public PDGExtractClosureAnalyzer(ProgramDependenceGraph pdg, IDocument doc, int selectionStart, int selectionLength) {
		this.pdg= pdg;
		selectedLines= calculateSelectedLines(doc, selectionStart, selectionLength);
	}

	/*
	 * This is mainly for testing purposes where we can test the line selections without the IDocument (UI-based)
	 */
	public PDGExtractClosureAnalyzer(ProgramDependenceGraph pdg, List<Integer> selectedLines) {
		this.pdg= pdg;
		this.selectedLines= selectedLines;
	}

	/**
	 * The method that does the bulk of the analysis. This method will calculate all the input and
	 * output dependencies for the selected lines and the "outside" world.
	 */
	public void analyzeSelection() {
		computeSelectedStatements();
		inputDataDependences= computeInput();
		outputDataDependences= computeOutput();
		closureLocalVariableNames= filterMethodParameters(computeLocalVariables());
	}

	private Set<String> filterMethodParameters(Set<String> localVariables) {
		Set<String> parameterNames= new HashSet<String>();
		for (DataDependence data : inputDataDependences) {
			parameterNames.addAll(data.getLocalVariableNames());
		}
		localVariables.removeAll(parameterNames); // Anything that is passed in as a parameter should not be redeclared
		return localVariables;
	}

	public List<DataDependence> getInputDataDependences() {
		return inputDataDependences;
	}

	public List<DataDependence> getOutputDataDependences() {
		return outputDataDependences;
	}

	public Set<String> getClosureLocalVariableNames() {
		return closureLocalVariableNames;
	}

	private void computeSelectedStatements() {
		for (PDGNode node : Iterator2Iterable.make(pdg.iterator())) {
			for (Integer line : selectedLines) {
				if (node.isOnLine(line))
					selectedStatements.add(node);
			}
		}
	}

	private boolean notPartOfInternalNodes(PDGNode node) {
		for (PDGNode internalNode : selectedStatements) {
			if (internalNode == node)
				return false;
		}
		return true;
	}

	// Get all successor nodes that do not belong to the set of selected lines
	private List<DataDependence> computeOutput() {
		List<DataDependence> outputs= new ArrayList<DataDependence>();

		for (PDGNode node : selectedStatements) {
			for (PDGNode succ : Iterator2Iterable.make(pdg.getSuccNodes(node))) {
				if (notPartOfInternalNodes(succ))
					outputs.addAll(pdg.getEdgeLabels(node, succ));
			}
		}

		return outputs;
	}

	// Get all the predecessor nodes that do not belong to the set of selected lines
	private List<DataDependence> computeInput() {
		List<DataDependence> inputs= new ArrayList<DataDependence>();

		for (PDGNode node : selectedStatements) {
			for (PDGNode pred : Iterator2Iterable.make(pdg.getPredNodes(node))) {
				if (notPartOfInternalNodes(pred))
					inputs.addAll(pdg.getEdgeLabels(pred, node));
			}
		}
		return inputs;
	}

	private Set<String> computeLocalVariables() {
		Set<String> localVariables= new HashSet<String>();
		for (PDGNode node : selectedStatements) {
			localVariables.addAll(node.defs());
		}
		return localVariables;
	}

	private List<Integer> calculateSelectedLines(IDocument doc, int selectionStart, int selectionLength) {
		List<Integer> lines= new ArrayList<Integer>();

		try {
			// Document is 0-based but we want 1-based, hence the +1
			int start= doc.getLineOfOffset(selectionStart) + 1;
			int end= doc.getLineOfOffset(selectionStart + selectionLength) + 1;
			for (int i= start; i <= end; i++) {
				lines.add(i);
			}

		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		return lines;
	}

	public Map<String, IBinding> transformDataDependencesToIBindings(ASTNode node, Collection<DataDependence> dependencies) {
		Set<String> names= extractNamesFromDependencies(dependencies);
		return BindingsFinder.findBindings(node, names);
	}

	private Set<String> extractNamesFromDependencies(Collection<DataDependence> dependencies) {
		Set<String> names= new HashSet<String>();

		for (DataDependence dependence : dependencies) {
			names.addAll(dependence.getLocalVariableNames());
		}

		return names;
	}
}
