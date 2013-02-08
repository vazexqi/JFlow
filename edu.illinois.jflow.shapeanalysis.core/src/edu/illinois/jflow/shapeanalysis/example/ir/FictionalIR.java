package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;

/**
 * This is an example of a fictional IR that resembles what is mentioned in
 * "Solving Shape-Analysis Problems in Languages with Destructive Updating". The purpose of this is
 * to illustrate how to build a shape analysis framewor without dealing with the complexities of
 * WALA (first).
 * 
 * @author nchen
 * 
 */
public class FictionalIR {
	PointerVariable lhs;

	PointerVariable rhs;

	Selector sel;

	public PointerVariable getLhs() {
		return lhs;
	}

	public PointerVariable getRhs() {
		return rhs;
	}

	public Selector getSel() {
		return sel;
	}
}
