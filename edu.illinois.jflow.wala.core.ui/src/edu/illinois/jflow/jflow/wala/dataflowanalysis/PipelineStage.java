package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.modref.ArrayLengthKey;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;
import com.ibm.wala.ipa.slicer.HeapExclusions;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Represents the stage of a pipeline. It will contain information about the stage, e.g., what needs
 * to come in, what needs to go out and what does it mod/ref, interprocedurally.
 * 
 * @author nchen
 * 
 */
public class PipelineStage {

	private int stageNumber;

	private ProgramDependenceGraph pdg;

	private List<Integer> selectedLines; // This is 1-based following how things "look" in the editor

	private List<PDGNode> selectedStatements= new ArrayList<PDGNode>(); // The actual selected statements (PDGNodes) in the editor

	// These are data dependencies that have to be explicitly tracked and passed/received during the transformation, i.e., they are expressable in code

	private List<DataDependence> inputDataDependences; // Think of these as method parameters

	private List<DataDependence> outputDataDependences; // Think of these as method return value(S) <-- yes possibly values!

	private Set<String> closureLocalVariableNames;

	// These are data dependencies that have to be implicitly tracked for feasibility but they are not visible in source code

	private Set<PointerKey> refs= new HashSet<PointerKey>();

	private Set<PointerKey> mods= new HashSet<PointerKey>();

	// These are the ignored methods that we do not consider (makes the analysis unsound) so we tell the user

	private Set<MethodReference> ignoreds= new HashSet<MethodReference>();

	/*
	 * Convenience method to create a new pipeline stage and begin the analysis immediately
	 */
	public static PipelineStage makePipelineStage(ProgramDependenceGraph pdg, int stageNumber, List<Integer> selectedLines) {
		PipelineStage temp= new PipelineStage(pdg, stageNumber, selectedLines);
		temp.analyzeSelection();
		return temp;
	}

	public PipelineStage(ProgramDependenceGraph pdg, int stageNumber, List<Integer> selectedLines) {
		this.pdg= pdg;
		this.stageNumber= stageNumber;
		this.selectedLines= selectedLines;
	}

	public void analyzeSelection() {
		computeSelectedStatements();
		inputDataDependences= computeInput();
		outputDataDependences= computeOutput();
		closureLocalVariableNames= filterMethodParameters(computeLocalVariables());
	}

	private void computeSelectedStatements() {
		for (PDGNode node : Iterator2Iterable.make(pdg.iterator())) {
			for (Integer line : selectedLines) {
				if (node.isOnLine(line))
					selectedStatements.add(node);
			}
		}
	}

	// Get all successor nodes that do not belong to the set of selected lines
	private List<DataDependence> computeOutput() {
		List<DataDependence> outputs= new ArrayList<DataDependence>();

		for (PDGNode node : selectedStatements) {
			for (PDGNode succ : Iterator2Iterable.make(pdg.getSuccNodes(node))) {
				if (notPartOfInternalNodes(succ))
					outputs.addAll(pdg.getEdgeLabels(node, succ));
			}
		}

		return outputs;
	}

	// Get all the predecessor nodes that do not belong to the set of selected lines
	private List<DataDependence> computeInput() {
		List<DataDependence> inputs= new ArrayList<DataDependence>();

		for (PDGNode node : selectedStatements) {
			for (PDGNode pred : Iterator2Iterable.make(pdg.getPredNodes(node))) {
				if (notPartOfInternalNodes(pred))
					inputs.addAll(pdg.getEdgeLabels(pred, node));
			}
		}
		return inputs;
	}

	private boolean notPartOfInternalNodes(PDGNode node) {
		for (PDGNode internalNode : selectedStatements) {
			if (internalNode == node)
				return false;
		}
		return true;
	}

	private Set<String> computeLocalVariables() {
		Set<String> localVariables= new HashSet<String>();
		for (PDGNode node : selectedStatements) {
			localVariables.addAll(node.defs());
		}
		return localVariables;
	}

	private Set<String> filterMethodParameters(Set<String> localVariables) {
		Set<String> parameterNames= new HashSet<String>();
		for (DataDependence data : inputDataDependences) {
			parameterNames.addAll(data.getLocalVariableNames());
		}
		localVariables.removeAll(parameterNames); // Anything that is passed in as a parameter should not be redeclared
		return localVariables;
	}

	public List<DataDependence> getInputDataDependences() {
		return inputDataDependences;
	}

	public List<DataDependence> getOutputDataDependences() {
		return outputDataDependences;
	}

	public Set<String> getClosureLocalVariableNames() {
		return closureLocalVariableNames;
	}

	public List<PDGNode> getSelectedStatements() {
		return selectedStatements;
	}

	public int getStageNumber() {
		return stageNumber;
	}

	private List<SSAInstruction> retrieveAllSSAInstructions() {
		List<SSAInstruction> ssaInstructions= new ArrayList<SSAInstruction>();

		for (PDGNode node : selectedStatements) {
			if (node instanceof Statement) {
				Statement statement= (Statement)node;
				ssaInstructions.addAll(statement.retrieveAllSSAInstructions());
			}
		}

		return ssaInstructions;
	}

	// Store these values in the outer class so that it is easier for testing

	private CGNode cgNode;

	private PointerAnalysis pointerAnalysis;

	private ModRef modref;

	private DelegatingExtendedHeapModel heapModel;

	private CallGraph callGraph;

	private Map<CGNode, OrdinalSet<PointerKey>> mod;

	private Map<CGNode, OrdinalSet<PointerKey>> ref;

	private Map<CGNode, OrdinalSet<MethodReference>> ignored;

	private HeapExclusions exclusions;


	public CGNode getCgNode() {
		return cgNode;
	}

	public PointerAnalysis getPointerAnalysis() {
		return pointerAnalysis;
	}

	public ModRef getModref() {
		return modref;
	}

	public DelegatingExtendedHeapModel getHeapModel() {
		return heapModel;
	}

	public CallGraph getCallGraph() {
		return callGraph;
	}

	public Map<CGNode, OrdinalSet<PointerKey>> getMod() {
		return mod;
	}

	public Map<CGNode, OrdinalSet<PointerKey>> getRef() {
		return ref;
	}

	public HeapExclusions getExclusions() {
		return exclusions;
	}

	// Modref analysis
	// Though this might look more complicated, we intentionally split this up (not doing modref upfront).
	// This facilitates a staged approach to determining feasibility of each pipeline stage and also makes
	// it easier to test in isolation.
	void computeHeapDependencies(CGNode cgNode, CallGraph callGraph, PointerAnalysis pointerAnalysis, ModRef modref, Map<CGNode, OrdinalSet<PointerKey>> mod, Map<CGNode, OrdinalSet<PointerKey>> ref,
			Map<CGNode, OrdinalSet<MethodReference>> ignored) {
		this.cgNode= cgNode;
		this.callGraph= callGraph;
		this.pointerAnalysis= pointerAnalysis;
		this.modref= modref;
		this.mod= mod;
		this.ignored= ignored;
		this.ref= ref;
		this.heapModel= new DelegatingExtendedHeapModel(pointerAnalysis.getHeapModel());
		PipelineStageModRef pipelineStageModRef= new PipelineStageModRef();
		pipelineStageModRef.computeHeapDependencies();
	}



	class PipelineStageModRef {

		void computeHeapDependencies() {
			computeRefs();
			computeMods();
			computeIgnores();
		}

		private void computeIgnores() {
			List<SSAInstruction> instructions= retrieveAllSSAInstructions();
			for (SSAInstruction instruction : instructions) {
				if (instruction instanceof SSAAbstractInvokeInstruction) {
					SSAAbstractInvokeInstruction call= (SSAAbstractInvokeInstruction)instruction;
					CallSiteReference callSite= call.getCallSite();
					Set<CGNode> possibleTargets= callGraph.getPossibleTargets(cgNode, callSite);
					for (CGNode target : possibleTargets) {
						OrdinalSet<MethodReference> ordinalSet= ignored.get(target);
						ignoreds.addAll(OrdinalSet.toCollection(ordinalSet));
					}
				}
			}

		}

		private void computeMods() {
			List<SSAInstruction> instructions= retrieveAllSSAInstructions();
			for (SSAInstruction instruction : instructions) {
				// These are direct modifications x.f = <something>
				Set<PointerKey> instructionMod= modref.getMod(cgNode, heapModel, pointerAnalysis, instruction, null);
				mods.addAll(instructionMod);

				// These are indirect modifications through calls
				if (instruction instanceof SSAAbstractInvokeInstruction) {
					SSAAbstractInvokeInstruction call= (SSAAbstractInvokeInstruction)instruction;
					CallSiteReference callSite= call.getCallSite();
					Set<CGNode> possibleTargets= callGraph.getPossibleTargets(cgNode, callSite);
					for (CGNode target : possibleTargets) {
						OrdinalSet<PointerKey> ordinalSet= mod.get(target);
						mods.addAll(OrdinalSet.toCollection(ordinalSet));
					}
				}
			}
		}

		private void computeRefs() {
			List<SSAInstruction> instructions= retrieveAllSSAInstructions();
			for (SSAInstruction instruction : instructions) {
				// These are direct references x.f
				Set<PointerKey> instructionRef= modref.getRef(cgNode, heapModel, pointerAnalysis, instruction, null);
				refs.addAll(instructionRef);

				// These are indirect modifications through calls
				if (instruction instanceof SSAAbstractInvokeInstruction) {
					SSAAbstractInvokeInstruction call= (SSAAbstractInvokeInstruction)instruction;
					CallSiteReference callSite= call.getCallSite();
					Set<CGNode> possibleTargets= callGraph.getPossibleTargets(cgNode, callSite);
					for (CGNode target : possibleTargets) {
						OrdinalSet<PointerKey> ordinalSet= ref.get(target);
						refs.addAll(OrdinalSet.toCollection(ordinalSet));
					}
				}
			}
		}
	}

	/*
	 * REMEMBER that we are not doing anything flow-sensitive inside the method bodies. Thus, we are not
	 * really tracking (R,W) (W,R) or (W,W) pairs – those require flow sensitivity. Instead, we are computing
	 * the "interference" of heap accesses and warning if there is a potential that there could be a race.
	 */

	public Set<PointerKey> getRefs() {
		return refs;
	}

	public Set<PointerKey> getMods() {
		return mods;
	}

	public Set<MethodReference> getIgnoreds() {
		return ignoreds;
	}

	// For some simple eyeballing statistics of the "shape" of the mod/ref

	public String getPrettyPrintMods() {
		return prettyPrint(mods);
	}

	public String getPrettyPrintRefs() {
		return prettyPrint(refs);
	}

	public String getPrettyPrintIgnored() {
		StringBuilder sb= new StringBuilder();
		for (MethodReference mRef : ignoreds) {
			sb.append(mRef.toString());
			sb.append(String.format("%n"));
		}
		return sb.toString();
	}

	private String prettyPrint(Set<PointerKey> set) {
		StringBuilder sb= new StringBuilder();

		/**
		 * A bit messy but creating function generators in Java is even messier.
		 */
		List<PointerKey> instanceFieldKeys= filter(set, new Predicate<PointerKey>() {
			@Override
			public boolean test(PointerKey t) {
				return t instanceof InstanceFieldKey;
			}
		});
		Collections.sort(instanceFieldKeys, new InstanceStringComparator());
		addListToBuffer("InstanceFieldKeys", instanceFieldKeys, sb);

		List<PointerKey> arrayLengthKeys= filter(set, new Predicate<PointerKey>() {
			@Override
			public boolean test(PointerKey t) {
				return t instanceof ArrayLengthKey;
			}
		});
		Collections.sort(arrayLengthKeys, new InstanceStringComparator());
		addListToBuffer("ArrayLengthKeys", arrayLengthKeys, sb);

		List<PointerKey> arrayContentsKeys= filter(set, new Predicate<PointerKey>() {
			@Override
			public boolean test(PointerKey t) {
				return t instanceof ArrayContentsKey;
			}
		});
		Collections.sort(arrayContentsKeys, new InstanceStringComparator());
		addListToBuffer("ArrayContentsKeys", arrayContentsKeys, sb);

		List<PointerKey> staticFieldKeys= filter(set, new Predicate<PointerKey>() {

			@Override
			public boolean test(PointerKey t) {
				return t instanceof StaticFieldKey;
			}
		});
		addListToBuffer("Static Fields", staticFieldKeys, sb);

		return sb.toString();
	}

	private void addListToBuffer(String header, List<PointerKey> instanceFieldKeys, StringBuilder sb) {
		sb.append(String.format("%s%n%n", header));

		for (PointerKey pointerKey : instanceFieldKeys) {
			sb.append(String.format("%s%n", pointerKey.toString()));
			sb.append(String.format("-- points to -->%n"));
			OrdinalSet<InstanceKey> pointsToSet= pointerAnalysis.getPointsToSet(pointerKey);
			sb.append(String.format("%s%n", pointsToSet.toString()));
			sb.append(String.format("%n"));
		}
	}

	private List<PointerKey> filter(Set<PointerKey> set, Predicate<PointerKey> filter) {
		return Predicate.filter(set.iterator(), filter);
	}

	private final class InstanceStringComparator implements Comparator<PointerKey> {
		@Override
		public int compare(PointerKey o1, PointerKey o2) {
			AbstractFieldPointerKey key1= (AbstractFieldPointerKey)o1;
			AbstractFieldPointerKey key2= (AbstractFieldPointerKey)o2;
			return key1.getInstanceKey().toString().compareTo(key2.getInstanceKey().toString());
		}
	}
}
