package edu.illinois.jflow.shapeanalysis.example.ir;

import com.ibm.wala.fixpoint.UnaryOperator;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;

/**
 * This is an example of a fictional IR that resembles what is mentioned in
 * "Solving Shape-Analysis Problems in Languages with Destructive Updating". The purpose of this is
 * to illustrate how to build a shape analysis framework without dealing with the complexities of
 * WALA (first).
 * 
 * @author nchen
 * 
 */
public abstract class FictionalIR<T> {
	PointerVariable lhs;

	PointerVariable rhs;

	Selector sel;

	boolean hasInitialValue;

	T initialValue;

	public PointerVariable getLhs() {
		return lhs;
	}

	public PointerVariable getRhs() {
		return rhs;
	}

	public Selector getSel() {
		return sel;
	}

	public boolean hasInitialValue() {
		return hasInitialValue;
	}

	public T getInitialValue() {
		return initialValue;
	}

	public void setInitialValue(T value) {
		this.initialValue= value;
		this.hasInitialValue= true;
	}

	@Override
	public String toString() {
		return "IR that has no effect on heap";
	}

	public UnaryOperator<StaticShapeGraph> getTransferFunction() {
		// This is the identity function for StaticShapeGraphs
		return SSGIdentity.instance();
	}

	private final static class SSGIdentity extends UnaryOperator<StaticShapeGraph> {
		private static final SSGIdentity SINGLETON= new SSGIdentity();

		public static SSGIdentity instance() {
			return SINGLETON;
		}

		@Override
		public byte evaluate(StaticShapeGraph lhs, StaticShapeGraph rhs) {
			if (lhs.sameValue(rhs)) {
				return NOT_CHANGED;
			}
			else {
				lhs.copyState(lhs);
				return CHANGED;
			}
		}

		@Override
		public int hashCode() {
			return "SSGIdentity".hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof FictionalIR.SSGIdentity);
		}

		@Override
		public String toString() {
			return "StaticShapeGraph identity transfer function";
		}

		@Override
		public boolean isIdentity() {
			return true;
		}

	}
}
