package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.cast.java.analysis.typeInference.AstJavaTypeInference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.labeled.NumberedLabeledEdgeManager;
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
public class ProgramDependenceGraph extends SlowSparseNumberedLabeledGraph<PDGNode, String> {

	private static final String DEFAULT_LABEL= "";

	public static boolean DEBUG= true;

	private IR ir;

	private Map<Integer, MethodParameter> valueNumber2MethodParameters;

	// Maps a line number (in the text editor) to a statement
	private Map<Integer, Statement> sourceLineMapping;

	// Maps the particular SSAInstruction to the containing statement
	private Map<SSAInstruction, Statement> instruction2Statement;

	// Maps the particular SSAInstruction to its index in the IR instruction array
	// The index is needed for determining the local names
	private Map<SSAInstruction, Integer> instruction2Index;

	// The original doc where this program dependence graph was constructed from
	private IDocument doc;

	private AstJavaTypeInference typeInferrer;

	public static ProgramDependenceGraph make(IR ir, IClassHierarchy classHierarchy) throws InvalidClassFileException {
		ProgramDependenceGraph g= new ProgramDependenceGraph(ir, classHierarchy);
		g.populate();
		return g;
	}

	public static ProgramDependenceGraph makeWithSourceCode(IR ir, IClassHierarchy classHierarchy, IDocument doc) throws InvalidClassFileException {
		ProgramDependenceGraph g= new ProgramDependenceGraph(ir, classHierarchy);
		g.setDocument(doc);
		g.populate();
		return g;
	}

	public ProgramDependenceGraph(IR ir, IClassHierarchy classHierarchy) {
		super(DEFAULT_LABEL);

		valueNumber2MethodParameters= new HashMap<Integer, MethodParameter>();
		sourceLineMapping= new HashMap<Integer, Statement>();
		instruction2Statement= new HashMap<SSAInstruction, Statement>();
		instruction2Index= new HashMap<SSAInstruction, Integer>();

		this.ir= ir;

		typeInferrer= new AstJavaTypeInference(ir, classHierarchy, true);
	}

	private void setDocument(IDocument doc) {
		this.doc= doc;
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

	private void createStatementsFromInstructions() throws InvalidClassFileException {
		SSAInstruction[] instructions= ir.getInstructions();

		//0. Create statements for callee params
		for (int valueNumber : ir.getParameterValueNumbers()) {
			valueNumber2MethodParameters.put(valueNumber, new MethodParameter(valueNumber, ir.getParameterType(valueNumber - 1)));
		}

		//1. Create statements for normal instructions
		IMethod method= ir.getMethod();
		for (int index= 0; index < instructions.length; index++) {
			int lineNumber= getLineNumber(index, method);
			mapInstruction(lineNumber, instructions[index], index);
		}

		//2. Add PhiInstructions, which are treated differently and not included in the instruction index
		// Instead, they are only included in the basic blocks so we need to iterate over them. When we find a PhiInstruction,
		// we associate it with the first instruction we see. If there is no first instruction, then this is an empty basic block and we can skip it.
		for (ISSABasicBlock bb : ir.getControlFlowGraph()) {
			int firstInstructionIndex= bb.getFirstInstructionIndex();
			int lastInstructionIndex= bb.getLastInstructionIndex();

			if (firstInstructionIndex < 0)
				continue; // No instructions in this basic block, skip it

			SSAInstruction instruction= locateFirstValidInstruction(firstInstructionIndex, lastInstructionIndex);

			Statement statement= instruction2Statement.get(instruction);

			for (SSAPhiInstruction phi : Iterator2Iterable.make(bb.iteratePhis())) {
				statement.add(phi);
				instruction2Statement.put(phi, statement);
			}
		}

	}

	/*
	 * Need to loop through the instruction indices range since some of the instructions could be null
	 */
	private SSAInstruction locateFirstValidInstruction(int firstInstructionIndex, int lastInstructionIndex) {
		SSAInstruction[] instructions= ir.getInstructions();

		for (int index= firstInstructionIndex; index <= lastInstructionIndex; index++) {
			if (instructions[index] != null)
				return instructions[index];
		}
		return null;
	}

	private int getLineNumber(int index, IMethod method) throws InvalidClassFileException {
		int lineNumber= method.getLineNumber(index);
		return lineNumber;
	}

	private void createGraphNodes() {
		for (MethodParameter methodParameter : valueNumber2MethodParameters.values()) {
			addNode(methodParameter);
		}

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
	 * <li>Creates a mapping from instruction to their index in the IR instruction array.</li>
	 * </ol>
	 * 
	 * @param lineNumber The corresponding line number in the source file
	 * @param instruction The instruction we are handling
	 * @param index The index of the instruction in the IR instruction arra
	 */
	private void mapInstruction(int lineNumber, SSAInstruction instruction, int index) {
		if (DEBUG) {
			System.out.println("LINE: " + lineNumber + ": " + instruction);
		}
		if (instruction != null) {
			Statement statement= sourceLineMapping.get(lineNumber);
			if (emptyStatementForLine(statement)) {
				statement= new Statement(lineNumber);
				attachSourceCodeIfPossible(statement);
				sourceLineMapping.put(lineNumber, statement);
			}
			statement.add(instruction);
			instruction2Statement.put(instruction, statement);
			instruction2Index.put(instruction, index);
		}
	}

	private void attachSourceCodeIfPossible(Statement statement) {
		if (doc != null) {
			int sourceLineNumber= statement.getLineNumber();
			int lineNumber= sourceLineNumber - 1; //IDocument indexing is 0-based
			try {
				int lineOffset= doc.getLineOffset(lineNumber);
				int lineLength= doc.getLineLength(lineNumber);
				String sourceCode= doc.get(lineOffset, lineLength).trim();
				statement.setSourceCode(sourceCode);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean emptyStatementForLine(Statement statement) {
		return statement == null;
	}

	private void addDependencyEdges() {
		DefUse DU= new DefUse(ir);

		// Add dependencies from parameters
		for (SSAInstruction instruction : instruction2Statement.keySet()) {
			addDependencyIfApplicable(instruction);
		}

		// Add dependencies from instructions
		for (SSAInstruction instruction : instruction2Statement.keySet()) {
			Statement defStatement= instruction2Statement.get(instruction);
			for (int def= 0; def < instruction.getNumberOfDefs(); def++) {
				int SSAVariable= instruction.getDef(def);
				for (SSAInstruction use : Iterator2Iterable.make(DU.getUses(SSAVariable))) {
					Statement useStatement= instruction2Statement.get(use);
					Integer instructionIndex= instruction2Index.get(use);
					String variableName= SSAVariableToLocalNameIfPossible(instructionIndex, ir, SSAVariable);
					addEdge(defStatement, useStatement, variableName);
				}
			}
		}
	}

	private void addDependencyIfApplicable(SSAInstruction instruction) {
		if (instruction.getNumberOfUses() > 0) {
			for (int use= 0; use < instruction.getNumberOfUses(); use++) {
				addEdgeIfHasParamDependency(instruction.getUse(use), instruction);
			}
		}
	}

	private void addEdgeIfHasParamDependency(int use, SSAInstruction instruction) {
		MethodParameter methodParameter= valueNumber2MethodParameters.get(use);
		if (methodParameter != null) {
			Statement statement= instruction2Statement.get(instruction);
			Integer instructionIndex= instruction2Index.get(instruction);
			String variableName= SSAVariableToLocalNameIfPossible(instructionIndex, ir, use);
			addEdge(methodParameter, statement, variableName);
		}
	}

	private String SSAVariableToLocalNameIfPossible(Integer instructionIndex, IR ir, int SSAVariable) {
		StringBuilder sb= new StringBuilder();

		TypeAbstraction typeAbstraction= typeInferrer.getType(SSAVariable);
		TypeReference type= typeAbstraction.getTypeReference();
		if (type != null) {
			sb.append(String.format("%s ", type.toString()));
		} else {
			Assertions.UNREACHABLE("Type inference failed to detect the type for one of our variables!");
		}

		if (instructionIndex == null) {
			sb.append(String.format("v%d", SSAVariable));
		} else {
			String[] localNames= ir.getLocalNames(instructionIndex, SSAVariable);
			if (localNames != null) {
				sb.append(Arrays.toString(localNames));
			} else {
				sb.append(String.format("v%d", SSAVariable));
			}
		}
		return sb.toString();
	}

	// For testing purposes to more easily query the underlying graph structure
	// Do NOT use for general purposes since this might change
	////////////////////////////////////////////////////////////////////////////

	public NumberedLabeledEdgeManager<PDGNode, String> getEdgeManager() {
		return super.getEdgeManager();
	}
}
