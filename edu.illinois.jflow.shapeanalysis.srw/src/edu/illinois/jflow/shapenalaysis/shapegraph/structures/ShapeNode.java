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

	private static final PointerVariable phiVariable= new PointerVariable("phi");

	private static final ShapeNode phiNode= new ShapeNode(phiVariable);

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

	/////////////////////////////////////////////////////////////////////////
	// Helper functions - most of these will return a new ShapeNode instance 
	// to make it easier to update without clobbering previous values
	////////////////////////////////////////////////////////////////////////

	public ShapeNode removeName(PointerVariable toRemove) {
		assert !toRemove.equals(phiVariable); // Cannot remove a phi variable

		ShapeNode newNode= new ShapeNode(this);
		newNode.name.remove(toRemove); // Try to remove the name
		if (newNode.name.isEmpty()) { // We have remove all variables and now it must point to the phi node
			newNode.name.add(phiVariable);
		}
		return newNode;
	}

	public ShapeNode addName(PointerVariable toAdd) {
		assert !toAdd.equals(phiVariable); // Cannot add a phi variable

		ShapeNode newNode= new ShapeNode(this);
		newNode.name.add(new PointerVariable(toAdd));
		if (newNode.containsName(phiVariable)) {
			newNode.name.remove(phiVariable);
		}
		return newNode;
	}

	public boolean containsName(PointerVariable toCheck) {
		return name.contains(toCheck);
	}

	public ShapeNode addNameIfContains(PointerVariable toAdd, PointerVariable toMatch) {
		ShapeNode newNode= new ShapeNode(this);
		if (containsName(toMatch)) {
			newNode.name.add(toAdd);
		}
		return newNode;
	}

	// Although compatible is defined for a list of ShapeNode, in the analysis we only ever use pairs and triples so let us take advantage of that
	public static boolean isCompatible(ShapeNode s1, ShapeNode s2) {
		ShapeNode intersection= new ShapeNode(s1); // Create a copy since set intersection is a destructive modification
		intersection.name.retainAll(s2.name);
		return s1.equals(s2) || intersection.name.isEmpty();
	}

	public static boolean isCompatible(ShapeNode s1, ShapeNode s2, ShapeNode s3) {
		return isCompatible(s1, s2) && isCompatible(s2, s3) && isCompatible(s1, s3);
	}

	@Override
	public int hashCode() {
		final int prime= 31;
		int result= 1;
		result= prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/////////////////////////////////////////////////////////////////////////////
	// IMPORTANT
	// The definition of not equals is not just !equals. There is a second clause
	// that checks for phi. Use this for compatiblity checks in x := y.sel
	/////////////////////////////////////////////////////////////////////////////
	public boolean semanticallyNotEquals(ShapeNode other) {
		boolean isPhiOnly= false;

		if (this.name.size() == 1 && other.name.size() == 1) {
			isPhiOnly= this.name.contains(phiVariable) && other.name.contains(phiVariable);
		}

		return !this.equals(other) || isPhiOnly;
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

		assert !this.name.isEmpty() && !other.name.isEmpty(); // Cannot have empty sets. It must at least contain the phi value

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
