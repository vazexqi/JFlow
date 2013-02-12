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

	// variableEdges and selectorEdges are pretty standard but isShared needs some clarification
	// By default, we are not going to put all possible ShapeNodes into isShared. If a ShapeNode A is
	// not inside isShared, we assume that by default it is not shared. If A is inside the map, we retrieve its value.
	// There is no checking to ensure that the A is even in the set of ShapeNodes. It just blatantly answers "no" for anything not in its
	// keyset. This is something to keep in mind while checking for shared ShapeNodes.
	Map<ShapeNode, Boolean> isShared= new HashMap<ShapeNode, Boolean>();

	public Set<VariableEdge> getVariableEdges() {
		return variableEdges;
	}

	public Set<SelectorEdge> getSelectorEdges() {
		return selectorEdges;
	}

	public Map<ShapeNode, Boolean> getIsShared() {
		return isShared;
	}

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

	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append(String.format("Variable Edges:%n"));
		for (VariableEdge ve : variableEdges) {
			sb.append(String.format("%s%n", ve));
		}
		sb.append(String.format("%n"));

		sb.append(String.format("Selector Edges:%n"));
		for (SelectorEdge se : selectorEdges) {
			sb.append(String.format("%s%n", se));
		}
		return sb.toString();
	}
}
