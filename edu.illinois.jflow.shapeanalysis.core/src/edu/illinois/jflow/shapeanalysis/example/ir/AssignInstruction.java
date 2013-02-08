package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;

/*
 * x := y
 */
public final class AssignInstruction extends FictionalIR {
	public AssignInstruction(PointerVariable lhs, PointerVariable rhs) {
		this.lhs= lhs;
		this.rhs= rhs;
	}
}
