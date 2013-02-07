package edu.illinois.jflow.shapenalaysis.shapegraph.structures;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an object in the heap named by the set of pointer variables pointing to it.
 * 
 * @author nchen
 * 
 */
public final class ShapeNode {
	final Set<PointerVariable> name= new HashSet<PointerVariable>();

	// Special node to represent the "primordial soup" of all objects
	public static ShapeNode phiNode= new ShapeNode(new PointerVariable("phi"));

	public ShapeNode(PointerVariable variable) {
		name.add(variable);
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
		ShapeNode other= (ShapeNode)obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
