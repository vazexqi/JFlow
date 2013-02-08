package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;

/*
 * x := new instruction
 */
public final class NewInstruction extends FictionalIR {
	public NewInstruction(PointerVariable lhs) {
		this.lhs= lhs;
	}
}
