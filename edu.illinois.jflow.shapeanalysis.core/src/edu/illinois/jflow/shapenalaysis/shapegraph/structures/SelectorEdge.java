package edu.illinois.jflow.shapenalaysis.shapegraph.structures;

/**
 * The triple representing the connection between ShapeNode s and ShapeNode t through the Selector
 * sel.
 * 
 * @author nchen
 * 
 */
public final class SelectorEdge {
	final ShapeNode s;

	final Selector sel;

	final ShapeNode t;

	public SelectorEdge(ShapeNode s, Selector sel, ShapeNode t) {
		this.s= s;
		this.sel= sel;
		this.t= t;
	}

	@Override
	public int hashCode() {
		final int prime= 31;
		int result= 1;
		result= prime * result + ((s == null) ? 0 : s.hashCode());
		result= prime * result + ((sel == null) ? 0 : sel.hashCode());
		result= prime * result + ((t == null) ? 0 : t.hashCode());
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
		SelectorEdge other= (SelectorEdge)obj;
		if (s == null) {
			if (other.s != null)
				return false;
		} else if (!s.equals(other.s))
			return false;
		if (sel == null) {
			if (other.sel != null)
				return false;
		} else if (!sel.equals(other.sel))
			return false;
		if (t == null) {
			if (other.t != null)
				return false;
		} else if (!t.equals(other.t))
			return false;
		return true;
	}
}
