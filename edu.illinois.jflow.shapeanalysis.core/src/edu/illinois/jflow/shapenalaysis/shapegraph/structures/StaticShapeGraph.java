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

	public void addVariableEdge(VariableEdge ve) {
		variableEdges.add(ve);
	}

	public void addSelectorEdge(SelectorEdge se) {
		selectorEdges.add(se);
	}

	public void addIsSharedMapping(ShapeNode node, Boolean isShared) {
		this.isShared.put(node, isShared);
	}

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

	// Follow the convention of fellow AbstractVariable descendants and create this new method called
	// sameValue(...) to test for content equality of objects.
	public boolean sameValue(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		StaticShapeGraph other= (StaticShapeGraph)obj;

		// Skip checking for nullness since by default we initialize everything
		boolean areVariableEdgesSame= false, areSelectorEdgesSame= false, areIsSharedSame= false;

		areVariableEdgesSame= variableEdges.equals(other.variableEdges);
		areSelectorEdgesSame= selectorEdges.equals(other.selectorEdges);
		areIsSharedSame= isShared.equals(other.isShared);

		// Check if VariableEdges are equal
		return areVariableEdgesSame && areSelectorEdgesSame && areIsSharedSame;
	}

}
