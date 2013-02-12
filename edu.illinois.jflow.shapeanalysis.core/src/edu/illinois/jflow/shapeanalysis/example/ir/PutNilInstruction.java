package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;

/*
 * x.sel := nil instruction
 */
public final class PutNilInstruction extends FictionalIR<StaticShapeGraph> {
	public PutNilInstruction(PointerVariable lhs, Selector sel) {
		this.lhs= lhs;
		this.sel= sel;
		this.rhs= null;
	}

	@Override
	public String toString() {
		return lhs + "." + sel + " := nil";
	}
}
