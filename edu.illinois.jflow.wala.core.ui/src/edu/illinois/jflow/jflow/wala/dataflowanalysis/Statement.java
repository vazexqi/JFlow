package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Pair;

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
	public static final Integer UNKNOWN_INSTRUCTION_INDEX= -1; // To support Phi instructions and other "non-real" instructions

	private final int lineNumber;

	private String sourceCode= "";

	private IR ir;

	private List<Pair<? extends SSAInstruction, Integer>> instructions;


	public Statement(int lineNumber, IR ir) {
		instructions= new ArrayList<Pair<? extends SSAInstruction, Integer>>();
		this.lineNumber= lineNumber;
		this.ir= ir;
	}

	public void add(Pair<? extends SSAInstruction, Integer> ssaInstruction) {
		instructions.add(ssaInstruction);
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String toString() {
		StringBuilder sb= new StringBuilder();

		sb.append(String.format("LINE: %d%n", lineNumber));
		for (Pair<? extends SSAInstruction, Integer> instr : instructions) {
			sb.append(instr.fst);
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

	private List<String> getVariableNamesForDefs(Pair<? extends SSAInstruction, Integer> pair) {
		List<String> names= new ArrayList<String>();
		SSAInstruction instr= pair.fst;
		Integer index= pair.snd;

		int numDefs= instr.getNumberOfDefs();
		for (int i= 0; i < numDefs; i++) {
			int def= instr.getDef(i);
			String[] localNames= ir.getLocalNames(index, def);
			names.addAll(Arrays.asList(localNames));
		}
		return names;
	}

	@Override
	public List<String> defs() {
		List<String> defs= new ArrayList<String>();
		for (Pair<? extends SSAInstruction, Integer> pair : instructions) {
			defs.addAll(getVariableNamesForDefs(pair));
		}
		return defs;
	}
}
