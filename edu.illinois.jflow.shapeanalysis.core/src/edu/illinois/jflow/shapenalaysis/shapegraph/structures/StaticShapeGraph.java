package edu.illinois.jflow.shapenalaysis.shapegraph.structures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.fixpoint.AbstractVariable;

/**
 * Representation of a Static Shape Graph according to Solving Shape-Analysis Problems in Languages
 * with Destructive Updating by Sagiv et al.
 * 
 * Customized to extend AbstractVariable so it can be used in the WALA Dataflow Framework.
 * 
 * @author nchen
 * 
 */
public class StaticShapeGraph extends AbstractVariable<StaticShapeGraph> {

	Set<VariableEdge> variableEdges= new HashSet<VariableEdge>();

	Set<SelectorEdge> selectorEdges= new HashSet<SelectorEdge>();

	Map<ShapeNode, Boolean> isShared= new HashMap<ShapeNode, Boolean>();

	@Override
	public void copyState(StaticShapeGraph other) {
		if (other == null)
			throw new IllegalArgumentException("Cannot copy, the reference is null!");

		// Perform deep copy of each collection.
		variableEdges= new HashSet<VariableEdge>();
		for (VariableEdge ve : other.variableEdges) {
			variableEdges.add(new VariableEdge(ve));
		}

		selectorEdges= new HashSet<SelectorEdge>();
		for (SelectorEdge se : other.selectorEdges) {
			selectorEdges.add(new SelectorEdge(se));
		}

		isShared= new HashMap<ShapeNode, Boolean>();
		for (ShapeNode s : other.isShared.keySet()) {
			isShared.put(new ShapeNode(s), other.isShared.get(s));
		}

	}
}
