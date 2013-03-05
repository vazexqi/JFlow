package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.graph.labeled.SlowSparseNumberedLabeledGraph;

/**
 * This is a dependence graph that is a simplified version of what you will find in a traditional
 * Program Dependence Graph (PDG). The main job of this class is to map dependencies at the
 * statement level instead of individual WALA IR. This mapping allows us to map back to our source
 * from the WALA IR.
 * 
 * @author nchen
 * 
 */
public class ProgramDependenceGraph extends SlowSparseNumberedLabeledGraph<Statement, String> {

	public static boolean DEBUG= true;

	private IR ir;

	private Map<Integer, Statement> sourceLineMapping;

	private Map<SSAInstruction, Statement> instruction2Statement;

	public static ProgramDependenceGraph make(IR ir) throws InvalidClassFileException {
		ProgramDependenceGraph g= new ProgramDependenceGraph(ir);
		g.populate();
		return g;
	}

	public ProgramDependenceGraph(IR ir) {
		super("");
		sourceLineMapping= new HashMap<Integer, Statement>();
		instruction2Statement= new HashMap<SSAInstruction, Statement>();
		this.ir= ir;
	}

	private void populate() throws InvalidClassFileException {
		// 1. Get all the "normal" instructions - meaning that we exclude SSAPiInstruction, SSAPhiInstruction and SSAGetCaughtExceptionInstructions
		// 2. Collect each instruction into the statement object corresponding to its source line number.
		createStatementsFromInstructions();

		// 3. Make each statement into its own node.
		createGraphNodes();

		// 4. Set up the dependencies between each node.
		addDependencyEdges();

	}

	private void addDependencyEdges() {
		DefUse DU= new DefUse(ir);

		SSAInstruction[] instructions= ir.getInstructions();
		for (int index= 0; index < instructions.length; index++) {
			SSAInstruction instruction= instructions[index];
			if (instruction != null) {
				Statement defStatement= instruction2Statement.get(instruction);
				for (int def= 0; def < instruction.getNumberOfDefs(); def++) {
					int SSAVariable= instruction.getDef(def);
					for (SSAInstruction use : Iterator2Iterable.make(DU.getUses(SSAVariable))) {
						Statement useStatement= instruction2Statement.get(use);
						addEdge(defStatement, useStatement, SSAVariableToLocalNameIfPossible(index, ir, SSAVariable));
					}
				}
			}
		}

	}

	private String SSAVariableToLocalNameIfPossible(int instructionIndex, IR ir, int SSAVariable) {
		StringBuilder sb= new StringBuilder();
		String[] localNames= ir.getLocalNames(instructionIndex, SSAVariable);
		if (localNames != null) {
			sb.append(Arrays.toString(localNames));
		} else {
			sb.append(Integer.toString(SSAVariable));
		}
		return sb.toString();
	}

	private void createStatementsFromInstructions() throws InvalidClassFileException {
		IBytecodeMethod method= (IBytecodeMethod)ir.getMethod();
		SSAInstruction[] instructions= ir.getInstructions();
		for (int index= 0; index < instructions.length; index++) {
			int lineNumber= getLineNumber(index, method);
			mapInstruction(lineNumber, instructions[index]);
		}
	}

	private int getLineNumber(int index, IBytecodeMethod method) throws InvalidClassFileException {
		int byteCodeIndex= method.getBytecodeIndex(index);
		int lineNumber= method.getLineNumber(byteCodeIndex);
		return lineNumber;
	}

	private void createGraphNodes() {
		for (Statement statement : sourceLineMapping.values()) {
			addNode(statement);
		}
	}

	/**
	 * Manages bookkeeping of instructions.
	 * <ol>
	 * <li>Stores the instruction into the "right" statement. (Line number -&gt; statement -&gt;
	 * instruction(s))</li>
	 * <li>Creates a mapping from instruction to statement for fast access (instruction -&gt;
	 * statement)</li>
	 * </ol>
	 * 
	 * @param lineNumber The corresponding line number in the source file
	 * @param SSAInstruction The instruction we are handling
	 */
	private void mapInstruction(int lineNumber, SSAInstruction SSAInstruction) {
		if (DEBUG) {
			System.out.println("LINE: " + lineNumber + ": " + SSAInstruction);
		}
		if (SSAInstruction != null) {
			Statement statement= sourceLineMapping.get(lineNumber);
			if (statement == null) {
				statement= new Statement(lineNumber);
				sourceLineMapping.put(lineNumber, statement);
			}
			statement.add(SSAInstruction);
			instruction2Statement.put(SSAInstruction, statement);
		}
	}

	/**
	 * This method is only valid after the call to populate().
	 * 
	 * @param instruction SSAInstruction in our IR
	 * @return the corresponding statement that contains this instruction
	 */
	private int instructionToLineNumber(SSAInstruction instruction) {
		Statement statement= instruction2Statement.get(instruction);
		return statement.getLineNumber();
	}
}
