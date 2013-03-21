package edu.illinois.jflow.jflow.wala.dataflowanalysis;

public interface PDGNode {

	// A simplified human-readable string representation that will be used for testing mostly (comparison-based)
	public String getSimplifiedRepresentation();

	public boolean isOnLine(int lineNumber);
}
