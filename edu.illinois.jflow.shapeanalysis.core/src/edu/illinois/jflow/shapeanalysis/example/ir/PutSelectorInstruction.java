package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;

/* 
 * x.sel := y
 */
public final class PutSelectorInstruction extends FictionalIR {
	public PutSelectorInstruction(PointerVariable lhs, Selector sel, PointerVariable rhs) {
		this.lhs= lhs;
		this.sel= sel;
		this.rhs= rhs;
	}
}
