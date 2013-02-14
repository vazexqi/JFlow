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
 * x := y.sel
 */
public final class GetSelectorInstruction extends FictionalIR<StaticShapeGraph> {
	public GetSelectorInstruction(PointerVariable lhs, PointerVariable rhs, Selector sel) {
		this.lhs= lhs;
		this.sel= sel;
		this.rhs= rhs;
	}

	@Override
	public String toString() {
		return lhs + " := " + rhs + "." + sel;
	}

	@Override
	public UnaryOperator<StaticShapeGraph> getTransferFunction() {
		return this.new SSGGetSelector();
	}

	private final class SSGGetSelector extends UnaryOperator<StaticShapeGraph> {

		@Override
		public byte evaluate(StaticShapeGraph out, StaticShapeGraph in) {
			StaticShapeGraph next= new StaticShapeGraph();

			// VariableEdges 
			// 1) Copy over old edges
			for (VariableEdge ve : in.getVariableEdges()) {
				next.addVariableEdge(new VariableEdge(ve));
			}

			Set<ShapeNode> allThingsPointedByYThroughSelector= new HashSet<ShapeNode>();
			for (ShapeNode s : in.pointsToOfVariable(getRhs())) {
				allThingsPointedByYThroughSelector.addAll(in.pointsToOfShapeNodeThroughSelector(s, getSel()));
			}

			// 2) Materialize a new object for anything that y.sel points to
			for (ShapeNode s : allThingsPointedByYThroughSelector) {
				next.addVariableEdge(new VariableEdge(new PointerVariable(getLhs()), s.addName(getLhs())));
			}

			// 3) Update the variables to point to the newly materialized object
			for (ShapeNode s : allThingsPointedByYThroughSelector) {
				Set<PointerVariable> variables= in.variablesToShapeNode(s);
				for (PointerVariable v : variables) {
					next.addVariableEdge(new VariableEdge(new PointerVariable(v), s.addName(getLhs())));
				}
			}

			// SelectorEdges

			// 1) Remove outdated edges
			for (SelectorEdge se : in.getSelectorEdges()) {
				if (se.s.containsName(getRhs()) && se.sel.equals(getSel())) {
					// Skip this one since it it outdated
				} else {
					// Copy it over
					next.addSelectorEdge(new SelectorEdge(se.s, se.sel, se.t));
				}
			}

			// 2) Add selectors judiciously
			for (ShapeNode s : in.pointsToOfVariable(getRhs())) { // ny
				for (SelectorEdge original : in.selectorEdgesStartingFrom(s)) { // <ny, sel, nz>

					//Compatible in
					for (SelectorEdge candidate : in.selectorEdgesEndingAt(original.t)) {
						if (compatibleIn(original, candidate, in)) {
							next.addSelectorEdge(new SelectorEdge(candidate.s, candidate.sel, candidate.t.addName(getLhs())));
						}
					}

					//Compatible self
					for (SelectorEdge candidate : in.selectorEdgesEndingAt(original.t)) {
						if (compatibleSelf(original, candidate, in)) {
							next.addSelectorEdge(new SelectorEdge(candidate.s.addName(getLhs()), candidate.sel, candidate.t.addName(getLhs())));
						}
					}

					//Compatible out
					for (SelectorEdge candidate : in.selectorEdgesStartingFrom(original.t)) {
						if (compatibleOut(original, candidate, in)) {
							next.addSelectorEdge(new SelectorEdge(candidate.s.addName(getLhs()), candidate.sel, candidate.t));
						}
					}
				}
			}

			// isShared
			for (ShapeNode s : in.getIsShared().keySet()) {
				Boolean resultofShapeNode= in.isShared(s.removeName(getLhs()));
				next.getIsShared().put(new ShapeNode(s), resultofShapeNode);
			}

			if (!out.sameValue(next)) {
				out.copyState(next);
				return CHANGED;
			} else {
				return NOT_CHANGED;
			}

		}

		/////////////////////////////////////////////////////////////////////////
		// Refer to Figure 12
		// Because of the way I have separated things, unfortunately, all boolean 
		// conditions are evaluated (no short-cutting) occurs.
		/////////////////////////////////////////////////////////////////////////

		// s1 = <ny, sel, nz>
		// s2 = <nw, sel', nz>
		boolean compatibleIn(SelectorEdge s1, SelectorEdge s2, StaticShapeGraph in) {
			assert s1.t.equals(s2.t);

			ShapeNode ny= s1.s;
			ShapeNode nz= s1.t;
			ShapeNode nw= s2.s;

			if (nz.equals(nw)) // If they are equal then the compatibleSelf will handle it
				return false;

			boolean condition0= ShapeNode.isCompatible(ny, nz, nw);
			boolean condition1= nz.semanticallyNotEquals(nw);
			boolean condition2= (ny.equals(nw) && s1.sel.equals(s2.sel)) || in.isShared(nz);

			return condition0 && condition1 && condition2;
		}

		// s1 = <ny, sel, nz>
		// s2 = <nz, sel', nz>
		boolean compatibleSelf(SelectorEdge s1, SelectorEdge s2, StaticShapeGraph in) {
			assert s1.t.equals(s2.t);

			ShapeNode ny= s1.s;
			ShapeNode nz= s1.t;

			if (!s2.s.equals(s2.t)) // If they do not "match" then it will be handled by compatibleIn
				return false;

			boolean condition0= ShapeNode.isCompatible(ny, nz);
			boolean condition1= (ny.equals(nz) && s1.sel.equals(s2.sel)) || in.isShared(nz);

			return condition0 && condition1;
		}

		// s1 = <ny, sel, nz>
		// s2 = <nz, sel', nw>
		boolean compatibleOut(SelectorEdge s1, SelectorEdge s2, StaticShapeGraph in) {
			if (!s1.t.equals(s2.s)) // If they do not "match", then skip, don't handle
				return false;

			ShapeNode ny= s1.s;
			ShapeNode nz= s1.t;
			ShapeNode nw= s2.t;
			boolean condition0= ShapeNode.isCompatible(ny, nz, nw);
			boolean condition1= nz.semanticallyNotEquals(nw);
			boolean condition2= ny.semanticallyNotEquals(nz) || !s1.sel.equals(s2.sel);

			return condition0 && condition1 && condition2;
		}

		@Override
		public int hashCode() {
			return "SSGGetSelector".hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof GetSelectorInstruction.SSGGetSelector);
		}

		@Override
		public String toString() {
			return "StaticShapeGraph get selector transfer function";
		}

	}

}
