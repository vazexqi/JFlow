package edu.illinois.jflow.shapeanalysis.example;

import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.SelectorEdge;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.ShapeNode;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.VariableEdge;

public class SSGMeetOperator extends AbstractMeetOperator<StaticShapeGraph> {

	private static final AbstractMeetOperator<StaticShapeGraph> SINGLETON= new SSGMeetOperator();

	public static AbstractMeetOperator<StaticShapeGraph> instance() {
		return SINGLETON;
	}

	@Override
	public int hashCode() {
		return "SSGMeetOperator".hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof SSGMeetOperator);
	}

	@Override
	public String toString() {
		return "StaticShapeGraph meet operator function";
	}

	@Override
	public byte evaluate(StaticShapeGraph lhs, StaticShapeGraph[] rhs) {
		StaticShapeGraph U= new StaticShapeGraph();
		U.copyState(lhs);

		for (StaticShapeGraph ssg : rhs) {
			// Need deep copies of each
			for (VariableEdge ve : ssg.getVariableEdges()) {
				U.getVariableEdges().add(new VariableEdge(ve));
			}

			for (SelectorEdge se : ssg.getSelectorEdges()) {
				U.getSelectorEdges().add(new SelectorEdge(se));
			}

			// isShared is a bit more tricky. We need to add all the values from ssg. However, if U already
			// contains that value, then we need to perform an OR on that value.

			Map<ShapeNode, Boolean> UisShared= U.getIsShared();
			for (Entry<ShapeNode, Boolean> entry : ssg.getIsShared().entrySet()) {
				ShapeNode key= entry.getKey();
				if (UisShared.containsKey(key)) {
					UisShared.put(key, UisShared.get(key) || entry.getValue());
				} else {
					UisShared.put(new ShapeNode(entry.getKey()), entry.getValue());
				}
			}
		}

		if (!lhs.sameValue(U)) {
			lhs.copyState(U);
			return CHANGED;
		} else {
			return NOT_CHANGED;
		}
	}

	@Override
	public boolean isUnaryNoOp() {
		return false;
	}


}
