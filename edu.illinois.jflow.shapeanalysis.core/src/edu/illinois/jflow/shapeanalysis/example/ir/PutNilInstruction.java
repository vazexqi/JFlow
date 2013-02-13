package edu.illinois.jflow.shapeanalysis.example.ir;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.fixpoint.UnaryOperator;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.SelectorEdge;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.ShapeNode;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.VariableEdge;

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

	@Override
	public UnaryOperator<StaticShapeGraph> getTransferFunction() {
		return this.new SSGPutNil();
	}

	private final class SSGPutNil extends UnaryOperator<StaticShapeGraph> {

		@Override
		public byte evaluate(StaticShapeGraph out, StaticShapeGraph in) {
			StaticShapeGraph next= new StaticShapeGraph();

			// VariableEdges - no change, just copy over
			for (VariableEdge ve : in.getVariableEdges()) {
				next.addVariableEdge(new VariableEdge(ve));
			}

			// SelectorEdges
			for (SelectorEdge se : in.getSelectorEdges()) {
				if (se.s.containsName(getLhs()) && se.sel.equals(getSel())) {
					// Skip this one since it has been nullified
				} else {
					// Copy it over
					next.addSelectorEdge(new SelectorEdge(se.s, se.sel, se.t));
				}
			}

			// isShared
			Set<ShapeNode> allThingsPointedByXThroughSelector= new HashSet<ShapeNode>();
			for (ShapeNode s : in.pointsToOfVariable(getLhs())) {
				allThingsPointedByXThroughSelector.addAll(in.pointsToOfShapeNodeThroughSelector(s, getSel()));
			}

			for (ShapeNode s : in.getIsShared().keySet()) {
				if (allThingsPointedByXThroughSelector.contains(s)) {
					next.addIsSharedMapping(new ShapeNode(s), in.isShared(s) && in.iis(s));
				} else {
					next.addIsSharedMapping(new ShapeNode(s), in.isShared(s));
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
			return "SSGPutNil".hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof PutNilInstruction.SSGPutNil);
		}

		@Override
		public String toString() {
			return "StaticShapeGraph put nil transfer function";
		}

	}
}
