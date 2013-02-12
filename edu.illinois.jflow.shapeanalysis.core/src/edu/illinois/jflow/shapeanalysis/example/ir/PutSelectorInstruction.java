package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;

/* 
 * x.sel := y
 */
public final class PutSelectorInstruction extends FictionalIR<StaticShapeGraph> {
	public PutSelectorInstruction(PointerVariable lhs, Selector sel, PointerVariable rhs) {
		this.lhs= lhs;
		this.sel= sel;
		this.rhs= rhs;
	}

	@Override
	public String toString() {
		return lhs + "." + sel + " := " + rhs;
	}
}
