package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;

/*
 * x := nil instruction
 */
public final class AssignNilInstruction extends FictionalIR {
	public AssignNilInstruction(PointerVariable lhs) {
		this.lhs= lhs;
		this.sel= null;
		this.rhs= null;
	}
}
