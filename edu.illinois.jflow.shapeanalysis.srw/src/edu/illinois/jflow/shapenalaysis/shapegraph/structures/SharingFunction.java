package edu.illinois.jflow.shapenalaysis.shapegraph.structures;

import java.util.HashMap;

/**
 * Specialized map for the isShared function. Basically, for each ShapeNode, tell me if it is shared
 * or not. By default, we are not going to put all possible ShapeNodes into isShared. If a ShapeNode
 * A is not inside isShared, we assume that by default it is not shared. If A is inside the map, we
 * retrieve its value. There is no checking to ensure that the A is even in the set of ShapeNodes.
 * It just blatantly answers "no" for anything not in its keyset. This is something to keep in mind
 * while checking for shared ShapeNodes.
 * 
 * @author nchen
 * 
 */
public class SharingFunction extends HashMap<ShapeNode, Boolean> {

	private static final long serialVersionUID= 1L;

	public SharingFunction(SharingFunction other) {
		super(other);
	}

	public SharingFunction() {
		super();
	}

	@Override
	public Boolean get(Object arg0) {
		Boolean result= super.get(arg0);
		if (result == null) {
			return false;
		} else {
			return result;
		}
	}
}
