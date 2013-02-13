package edu.illinois.jflow.shapeanalysis.example.ir;

import com.ibm.wala.fixpoint.UnaryOperator;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.SelectorEdge;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.ShapeNode;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.VariableEdge;

/*
 * x := nil instruction
 */
public final class AssignNilInstruction extends FictionalIR<StaticShapeGraph> {
	public AssignNilInstruction(PointerVariable lhs) {
		this.lhs= lhs;
		this.sel= null;
		this.rhs= null;
	}

	@Override
	public String toString() {
		return lhs + " := nil";
	}

	@Override
	public UnaryOperator<StaticShapeGraph> getTransferFunction() {
		return this.new SSGAssignNil();
	}

	private final class SSGAssignNil extends UnaryOperator<StaticShapeGraph> {
		@Override
		public byte evaluate(StaticShapeGraph out, StaticShapeGraph in) {
			StaticShapeGraph next= new StaticShapeGraph();

			// VariableEdges
			for (VariableEdge ve : in.getVariableEdges()) {
				if (!ve.v.equals(getLhs())) {
					ShapeNode newName= ve.n.removeName(getLhs());
					next.addVariableEdge(new VariableEdge(new PointerVariable(ve.v), newName));
				}
			}

			// SelectorEdges
			for (SelectorEdge se : in.getSelectorEdges()) {
				ShapeNode newFrom= se.s.removeName(getLhs());
				ShapeNode newTo= se.t.removeName(getLhs());
				next.addSelectorEdge(new SelectorEdge(newFrom, new Selector(se.sel), newTo));
			}

			// isShared
			for (ShapeNode s : in.getIsShared().keySet()) {
				Boolean resultofShapeNode= in.getIsShared().get(s);
				Boolean resultOfShapeNodeWithNameRemoved= in.getIsShared().get(s.removeName(getLhs()));
				next.getIsShared().put(new ShapeNode(s), resultofShapeNode || resultOfShapeNodeWithNameRemoved);
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
			return "SSGAssignNil".hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof AssignNilInstruction.SSGAssignNil);
		}

		@Override
		public String toString() {
			return "StaticShapeGraph assign nil transfer function";
		}

	}
}
