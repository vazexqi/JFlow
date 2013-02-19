package edu.illinois.jflow.shapeanalysis.example.ir;

import com.ibm.wala.fixpoint.UnaryOperator;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.SelectorEdge;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.ShapeNode;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.VariableEdge;

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

	@Override
	public UnaryOperator<StaticShapeGraph> getTransferFunction() {
		return this.new SSGNew();
	}

	private final class SSGNew extends UnaryOperator<StaticShapeGraph> {

		@Override
		public byte evaluate(StaticShapeGraph out, StaticShapeGraph in) {
			StaticShapeGraph next= new StaticShapeGraph();

			// VariableEdges - copy over all old edges and add a new edge for the newly allocated x
			for (VariableEdge ve : in.getVariableEdges()) {
				next.addVariableEdge(ve);
			}
			PointerVariable x= new PointerVariable(getLhs());
			next.addVariableEdge(new VariableEdge(getLhs(), new ShapeNode(x)));

			// SelectorEdges - no change, just copy over
			for (SelectorEdge se : in.getSelectorEdges()) {
				next.addSelectorEdge(se);
			}

			// isShared - no change since the shared status of the newly allocated x is assumed to be false by default
			for (ShapeNode s : in.getIsShared().keySet()) {
				next.addIsSharedMapping(new ShapeNode(s), in.isShared(s));
			}

			if (!out.sameValue(next)) {
				out.copyState(next);
				return CHANGED;
			} else {
				return NOT_CHANGED;
			}
		}

		@Override
		public int hashCode() {
			return "SSGNew".hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof NewInstruction.SSGNew);
		}

		@Override
		public String toString() {
			return "[StaticShapeGraph new transfer function] " + NewInstruction.this.toString();
		}

	}
}
