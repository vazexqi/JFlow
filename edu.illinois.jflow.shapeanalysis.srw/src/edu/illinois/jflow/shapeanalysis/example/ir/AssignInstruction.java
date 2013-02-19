package edu.illinois.jflow.shapeanalysis.example.ir;

import com.ibm.wala.fixpoint.UnaryOperator;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.SelectorEdge;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.ShapeNode;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.VariableEdge;

/*
 * x := y
 */
public final class AssignInstruction extends FictionalIR<StaticShapeGraph> {
	public AssignInstruction(PointerVariable lhs, PointerVariable rhs) {
		this.lhs= lhs;
		this.rhs= rhs;
	}

	@Override
	public String toString() {
		return lhs + " := " + rhs;
	}

	@Override
	public UnaryOperator<StaticShapeGraph> getTransferFunction() {
		return this.new SSGAssign();
	}

	private final class SSGAssign extends UnaryOperator<StaticShapeGraph> {

		@Override
		public byte evaluate(StaticShapeGraph out, StaticShapeGraph in) {
			StaticShapeGraph next= new StaticShapeGraph();

			// VariablesEdges - Add new bindings for x
			for (VariableEdge ve : in.getVariableEdges()) {
				if (ve.n.containsName(getRhs())) {
					next.addVariableEdge(new VariableEdge(getLhs(), ve.n.addNameIfContains(getLhs(), getRhs())));
				}
			}
			// VariableEdges - Update old bindings for everything that y pointed to
			for (VariableEdge ve : in.getVariableEdges()) {
				next.addVariableEdge(new VariableEdge(ve.v, ve.n.addNameIfContains(getLhs(), getRhs())));
			}

			// SelectorEdges - Update everything that y pointed to also include x
			for (SelectorEdge se : in.getSelectorEdges()) {
				ShapeNode newSource= se.s.addNameIfContains(getLhs(), getRhs());
				ShapeNode newTo= se.t.addNameIfContains(getLhs(), getRhs());
				next.addSelectorEdge(new SelectorEdge(newSource, se.sel, newTo));
			}

			// isShared
			for (ShapeNode s : in.getIsShared().keySet()) {
				Boolean resultofShapeNode= in.isShared(s.removeName(getLhs()));
				next.addIsSharedMapping(new ShapeNode(s), resultofShapeNode);
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
			return "SSGAssign".hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof AssignInstruction.SSGAssign);
		}

		@Override
		public String toString() {
			return "[StaticShapeGraph assign transfer function] " + AssignInstruction.this.toString();
		}

	}

}
