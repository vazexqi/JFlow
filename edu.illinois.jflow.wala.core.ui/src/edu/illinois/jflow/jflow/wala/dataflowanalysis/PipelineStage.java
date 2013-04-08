package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * Represents the stage of a pipeline. It will contain information about the stage, e.g., what needs
 * to come in, what needs to go out and what does it mod/ref, interprocedurally.
 * 
 * @author nchen
 * 
 */
public class PipelineStage {
	private ProgramDependenceGraph pdg;

	private List<Integer> selectedLines; // This is 1-based following how things "look" in the editor

	private List<PDGNode> selectedStatements= new ArrayList<PDGNode>(); // The actual selected statements (PDGNodes) in the editor

	private List<DataDependence> inputDataDependences; // Think of these as method parameters

	private List<DataDependence> outputDataDependences; // Think of these as method return value(S) <-- yes possibly values!

	private Set<String> closureLocalVariableNames;

	/*
	 * Convenience method to create a new pipeline stage and begin the analysis immediately
	 */
	public static PipelineStage makePipelineStage(ProgramDependenceGraph pdg, List<Integer> selectedLines) {
		PipelineStage temp= new PipelineStage(pdg, selectedLines);
		temp.analyzeSelection();
		return temp;
	}

	public PipelineStage(ProgramDependenceGraph pdg, List<Integer> selectedLines) {
		this.pdg= pdg;
		this.selectedLines= selectedLines;
	}

	public void analyzeSelection() {
		computeSelectedStatements();
		inputDataDependences= computeInput();
		outputDataDependences= computeOutput();
		closureLocalVariableNames= filterMethodParameters(computeLocalVariables());
	}

	private void computeSelectedStatements() {
		for (PDGNode node : Iterator2Iterable.make(pdg.iterator())) {
			for (Integer line : selectedLines) {
				if (node.isOnLine(line))
					selectedStatements.add(node);
			}
		}
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

	private boolean notPartOfInternalNodes(PDGNode node) {
		for (PDGNode internalNode : selectedStatements) {
			if (internalNode == node)
				return false;
		}
		return true;
	}

	private Set<String> computeLocalVariables() {
		Set<String> localVariables= new HashSet<String>();
		for (PDGNode node : selectedStatements) {
			localVariables.addAll(node.defs());
		}
		return localVariables;
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

	public List<PDGNode> getSelectedStatements() {
		return selectedStatements;
	}

}
