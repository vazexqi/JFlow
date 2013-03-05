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
public class Statement {
	private final int lineNumber;

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
}
