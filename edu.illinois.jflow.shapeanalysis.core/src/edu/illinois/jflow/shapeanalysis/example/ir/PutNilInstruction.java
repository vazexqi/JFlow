package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;

/*
 * x.sel := nil instruction
 */
public final class PutNilInstruction extends FictionalIR {
	public PutNilInstruction(PointerVariable lhs, Selector sel) {
		this.lhs= lhs;
		this.sel= sel;
		this.rhs= null;
	}
}
