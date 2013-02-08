package edu.illinois.jflow.shapenalaysis.shapegraph.structures;

/**
 * Represents pointers in the program.
 * 
 * @author nchen
 * 
 */
public final class PointerVariable {
	final String name;

	public PointerVariable(String name) {
		this.name= name;
	}

	public PointerVariable(PointerVariable other) {
		this.name= other.name;
	}

	@Override
	public int hashCode() {
		final int prime= 31;
		int result= 1;
		result= prime * result + ((name == null) ? 0 : name.hashCode());
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
		PointerVariable other= (PointerVariable)obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
