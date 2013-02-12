package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;

/*
 * x := y
 */
public final class AssignInstruction extends FictionalIR<StaticShapeGraph> {
	public AssignInstruction(PointerVariable lhs, PointerVariable rhs) {
		this.lhs= lhs;
		this.rhs= rhs;
	}

	@Override
	public String toString() {
		return lhs + " := " + rhs;
	}
}
