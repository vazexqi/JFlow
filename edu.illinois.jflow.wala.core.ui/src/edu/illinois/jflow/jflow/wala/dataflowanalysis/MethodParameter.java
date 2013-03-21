package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import com.ibm.wala.types.TypeReference;

/**
 * Represents a parameter to a method
 * 
 * @author nchen
 * 
 */
public class MethodParameter implements PDGNode {

	private int valueNumber;

	private TypeReference parameterType;

	public MethodParameter(int valueNumber, TypeReference parameterType) {
		this.valueNumber= valueNumber;
		this.parameterType= parameterType;
	}

	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append("[" + valueNumber + "]");
		sb.append(" ");
		sb.append(parameterType.getName());
		return sb.toString();
	}

	@Override
	public String getSimplifiedRepresentation() {
		return parameterType.getName().toString();
	}

	@Override
	public boolean isOnLine(int lineNumber) {
		// By default all MethodParameters have no line numbers so we always return false
		return false;
	}
}
