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

	private static final ShapeNode phiNode= new ShapeNode(new PointerVariable("phi"));

	public ShapeNode(PointerVariable variable) {
		name.add(variable);
	}

	public ShapeNode(ShapeNode other) {
		// Perform deep copy
		for (PointerVariable p : other.name) {
			name.add(new PointerVariable(p));
		}
	}

	public static ShapeNode getPhiNode() {
		return phiNode;
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

	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append("N{");

		for (PointerVariable pv : name) {
			sb.append(pv + ",");
		}

		// Delete the last comma - the logic for just deleting the last comma is cleaner than special casing
		// Because all ShapeNodes are constructed from at least one PointerVariable, we know the loop above must run at least once
		sb.deleteCharAt(sb.length() - 1);

		sb.append("}");
		return sb.toString();
	}

}
