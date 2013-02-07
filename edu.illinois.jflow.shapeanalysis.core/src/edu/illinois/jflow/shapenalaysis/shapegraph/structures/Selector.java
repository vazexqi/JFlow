package edu.illinois.jflow.shapenalaysis.shapegraph.structures;

/**
 * Represents the name of a field.
 * 
 * @author nchen
 * 
 */
public final class Selector {
	final String selector;

	public Selector(String selector) {
		this.selector= selector;
	}

	@Override
	public int hashCode() {
		final int prime= 31;
		int result= 1;
		result= prime * result + ((selector == null) ? 0 : selector.hashCode());
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
		Selector other= (Selector)obj;
		if (selector == null) {
			if (other.selector != null)
				return false;
		} else if (!selector.equals(other.selector))
			return false;
		return true;
	}
}
