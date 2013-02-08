package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;

/*
 * x := y.sel
 */
public final class GetSelectorInstruction extends FictionalIR {
	public GetSelectorInstruction(PointerVariable lhs, Selector sel, PointerVariable rhs) {
		this.lhs= lhs;
		this.sel= sel;
		this.rhs= rhs;
	}
}
