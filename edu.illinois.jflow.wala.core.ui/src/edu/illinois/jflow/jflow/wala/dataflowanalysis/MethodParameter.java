package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
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

	@Override
	public List<String> defs() {
		return new ArrayList<String>();
	}

	@Override
	public Set<PointerKey> getRefs() {
		return Collections.emptySet();
	}

	@Override
	public Set<PointerKey> getMods() {
		return Collections.emptySet();
	}

	@Override
	public void addRef(PointerKey key) {
		// Does nothing
	}

	@Override
	public void addMod(PointerKey key) {
		// Does nothing
	}
}
