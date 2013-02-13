package edu.illinois.jflow.shapenalaysis.shapegraph.structures;

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

	SharingFunction isShared= new SharingFunction();

	public Set<VariableEdge> getVariableEdges() {
		return variableEdges;
	}

	public Set<SelectorEdge> getSelectorEdges() {
		return selectorEdges;
	}

	public Map<ShapeNode, Boolean> getIsShared() {
		return isShared;
	}

	public boolean isShared(ShapeNode n) {
		return isShared.get(n);
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

		isShared= new SharingFunction(other.isShared);
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
		if (variableEdges.isEmpty()) {
			sb.append("EMPTY");
		} else {
			for (VariableEdge ve : variableEdges) {
				sb.append(String.format("%s%n", ve));
			}
		}
		sb.append(String.format("%n"));

		sb.append(String.format("Selector Edges:%n"));
		if (selectorEdges.isEmpty()) {
			sb.append("EMPTY");
		} else {
			for (SelectorEdge se : selectorEdges) {
				sb.append(String.format("%s%n", se));
			}
		}
		sb.append(String.format("%n"));

		sb.append(String.format("Shared nodes%n"));
		if (isShared.isEmpty()) {
			sb.append("EMPTY");
		} else {
			// Only print out the ones that are true. Note that due to the updates (i.e., flipping from true -> false) we might see entries with false values. We don't print those out.
			boolean hasNonSharedShapeNodes= false;
			for (ShapeNode node : isShared.keySet()) {
				if (isShared.get(node)) {
					sb.append(String.format("%s%n", node));
				} else {
					hasNonSharedShapeNodes= true;
				}
			}
			if (hasNonSharedShapeNodes) {
				sb.append("Not empty but all shapenodes are not shared");
			}
		}

		return sb.toString();
	}

	///////////////////////////////////////////
	// Helper functions over static heap graphs
	///////////////////////////////////////////

	// See Figure 10 of the paper
	// induced-is-shared function - meaning that they are two objects pointing to ShapeNode l
	public boolean iis(ShapeNode l) {
		int count= 0;
		for (SelectorEdge se : selectorEdges) {
			if (se.hasAsTarget(l)) {
				if (ShapeNode.isCompatible(se.s, l)) { // Maybe this check can be removed since we are certain that we are not using spurious edges
					count++;
				}
			}
		}
		return count >= 2;
	}

	public Set<ShapeNode> pointsToOfVariable(PointerVariable x) {
		Set<ShapeNode> pointsTo= new HashSet<ShapeNode>();

		for (VariableEdge ve : variableEdges) {
			if (ve.v.equals(x)) {
				pointsTo.add(ve.n);
			}
		}

		return pointsTo;
	}

	public Set<ShapeNode> pointsToOfShapeNodeThroughSelector(ShapeNode s, Selector sel) {
		Set<ShapeNode> pointsTo= new HashSet<ShapeNode>();

		for (SelectorEdge se : selectorEdges) {
			if (se.hasAsSourceAndSelector(s, sel))
				pointsTo.add(se.t);
		}

		return pointsTo;
	}

	public Set<PointerVariable> variablesToShapeNode(ShapeNode pointee) {
		Set<PointerVariable> pointers= new HashSet<PointerVariable>();

		for (VariableEdge ve : variableEdges) {
			if (ve.n.equals(pointee)) {
				pointers.add(new PointerVariable(ve.v));
			}
		}

		return pointers;
	}

	public Set<SelectorEdge> selectorEdgesStartingFrom(ShapeNode start) {
		Set<SelectorEdge> edges= new HashSet<SelectorEdge>();

		for (SelectorEdge se : selectorEdges) {
			if (se.s.equals(start))
				edges.add(new SelectorEdge(se));
		}

		return edges;
	}

	public Set<SelectorEdge> selectorEdgesEndingAt(ShapeNode end) {
		Set<SelectorEdge> edges= new HashSet<SelectorEdge>();

		for (SelectorEdge se : selectorEdges) {
			if (se.t.equals(end))
				edges.add(new SelectorEdge(se));
		}

		return edges;
	}
}
