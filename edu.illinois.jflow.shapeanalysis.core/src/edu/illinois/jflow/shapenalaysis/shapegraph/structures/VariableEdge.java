package edu.illinois.jflow.shapenalaysis.shapegraph.structures;

/**
 * The pair representing the connection between a PointerVariable and the ShapeNode that it points
 * to.
 * 
 * @author nchen
 * 
 */
public final class VariableEdge {
	final PointerVariable v;

	final ShapeNode n;

	public VariableEdge(PointerVariable v, ShapeNode n) {
		this.v= v;
		this.n= n;
	}

	public VariableEdge(VariableEdge other) {
		this.v= other.v;
		this.n= other.n;
	}

	@Override
	public int hashCode() {
		final int prime= 31;
		int result= 1;
		result= prime * result + ((n == null) ? 0 : n.hashCode());
		result= prime * result + ((v == null) ? 0 : v.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VariableEdge other= (VariableEdge)obj;
		if (n == null) {
			if (other.n != null)
				return false;
		} else if (!n.equals(other.n))
			return false;
		if (v == null) {
			if (other.v != null)
				return false;
		} else if (!v.equals(other.v))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + v.toString() + "," + n.toString() + "]";
	}
}
