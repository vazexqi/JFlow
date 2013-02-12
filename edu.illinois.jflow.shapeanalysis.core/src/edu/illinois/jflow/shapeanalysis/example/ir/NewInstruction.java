package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;

/*
 * x := new instruction
 */
public final class NewInstruction extends FictionalIR<StaticShapeGraph> {
	public NewInstruction(PointerVariable lhs) {
		this.lhs= lhs;
	}

	@Override
	public String toString() {
		return lhs + " := new";
	}
}
