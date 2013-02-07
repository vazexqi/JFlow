package edu.illinois.jflow.shapenalaysis.shapegraph.structures;

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

	Set<VariableEdge> variableEdges;

	Set<SelectorEdge> selectorEdges;

	Map<ShapeNode, Boolean> isShared;

	@Override
	public void copyState(StaticShapeGraph v) {
	}

}
