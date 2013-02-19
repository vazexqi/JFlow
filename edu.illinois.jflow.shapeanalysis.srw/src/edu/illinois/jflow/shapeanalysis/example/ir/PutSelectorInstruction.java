package edu.illinois.jflow.shapeanalysis.example.ir;

import java.util.Set;

import com.ibm.wala.fixpoint.UnaryOperator;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.SelectorEdge;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.ShapeNode;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.VariableEdge;

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

	@Override
	public UnaryOperator<StaticShapeGraph> getTransferFunction() {
		return this.new SSGPut();
	}

	private final class SSGPut extends UnaryOperator<StaticShapeGraph> {

		@Override
		public byte evaluate(StaticShapeGraph out, StaticShapeGraph in) {
			StaticShapeGraph next= new StaticShapeGraph();

			// VariableEdges - no change, just copy over
			for (VariableEdge ve : in.getVariableEdges()) {
				next.addVariableEdge(ve);
			}

			// SelectorEdges - copy over old edges
			for (SelectorEdge se : in.getSelectorEdges()) {
				next.addSelectorEdge(se);
			}

			// SelectorEdges - add new edges
			Set<ShapeNode> allX= in.pointsToOfVariable(getLhs());
			Set<ShapeNode> allY= in.pointsToOfVariable(getRhs());
			for (ShapeNode x : allX) {
				for (ShapeNode y : allY) {
					if (ShapeNode.isCompatible(x, y)) {
						next.addSelectorEdge(new SelectorEdge(x, getSel(), y));
					}
				}
			}

			// isShared
			// TODO: This is a bit tricky, maybe refactor to less optimized version?

			// Copy all the old values over first. This is safe since the next for loop is using a || operation.
			// At best, it will give a true value. It will never turn something to a false value that wasn't originally false.
			for (ShapeNode s : in.getIsShared().keySet()) {
				next.addIsSharedMapping(new ShapeNode(s), in.isShared(s));
			}

			Set<ShapeNode> pointsToOfY= in.pointsToOfVariable(getRhs());
			for (ShapeNode s : pointsToOfY) {
				boolean originalSharing= in.isShared(s);
				boolean isIIS= next.iis(s);
				if (originalSharing || isIIS) {
					next.addIsSharedMapping(new ShapeNode(s), true);
				}
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
			return "SSGPut".hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof PutSelectorInstruction.SSGPut);
		}

		@Override
		public String toString() {
			return "[StaticShapeGraph put nil transfer function] " + PutSelectorInstruction.this.toString();
		}

	}
}
