package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;

/*
 * x := nil instruction
 */
public final class AssignNilInstruction extends FictionalIR<StaticShapeGraph> {
	public AssignNilInstruction(PointerVariable lhs) {
		this.lhs= lhs;
		this.sel= null;
		this.rhs= null;
	}

	@Override
	public String toString() {
		return lhs + " := nil";
	}
}
