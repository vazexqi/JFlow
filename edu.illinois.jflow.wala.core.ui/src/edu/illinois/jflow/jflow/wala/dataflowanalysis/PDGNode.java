package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

public interface PDGNode {

	// A simplified human-readable string representation that will be used for testing mostly (comparison-based)
	public String getSimplifiedRepresentation();

	public boolean isOnLine(int lineNumber);

	// For now we seem to only need the defs – perhaps we will need uses in the future
	public List<String> defs();

	public void addRef(PointerKey key);

	public void addMod(PointerKey key);

	public Set<PointerKey> getRefs();

	public Set<PointerKey> getMods();
}
