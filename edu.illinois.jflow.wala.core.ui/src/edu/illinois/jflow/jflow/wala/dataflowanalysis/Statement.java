package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.ssa.*;

/**
 * Represents a statement in the source code.
 * 
 * A statement contains a collection of WALA IR, all of which maps to that particular line number in
 * the source code.
 * 
 * @author nchen
 * 
 */
public class Statement implements PDGNode {
	private final int lineNumber;

	private String sourceCode= "";

	List<SSAInstruction> instructions;

	public Statement(int lineNumber) {
		instructions= new ArrayList<SSAInstruction>();
		this.lineNumber= lineNumber;
	}

	public void add(SSAInstruction ssaInstruction) {
		instructions.add(ssaInstruction);
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String toString() {
		StringBuilder sb= new StringBuilder();

		sb.append(String.format("LINE: %d%n", lineNumber));
		for (SSAInstruction instr : instructions) {
			sb.append(instr);
			sb.append("\n");
		}

		return sb.toString();
	}

	public String getSourceCode() {
		return sourceCode;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode= sourceCode;
	}

	@Override
	public String getSimplifiedRepresentation() {
		return sourceCode;
	}

	@Override
	public boolean isOnLine(int lineNumber) {
		return this.lineNumber == lineNumber;
	}
}
