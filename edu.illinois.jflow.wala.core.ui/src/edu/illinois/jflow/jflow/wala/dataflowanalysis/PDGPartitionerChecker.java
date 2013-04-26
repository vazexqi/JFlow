package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.illinois.jflow.wala.modref.JFlowModRef;

/**
 * Acts as a facade to check the validity of the selected statements. Checks in sequence:
 * <ol>
 * <li>Constructs the different stages, including the generator</li>
 * <li>Checks for loop-carried dependencies of scalar variables</li>
 * <li>Checks for interference of heap variables</li>
 * </ol>
 * 
 * @author nchen
 * 
 */
public class PDGPartitionerChecker {
	private ProgramDependenceGraph pdg; // Need a reference to the program dependence graph

	private List<PipelineStage> stages= new ArrayList<PipelineStage>();

	private Map<PDGNode, Integer> node2stage= new HashMap<PDGNode, Integer>();

	private JFlowModRef modref;

	private Map<CGNode, OrdinalSet<PointerKey>> mod;

	private Map<CGNode, OrdinalSet<PointerKey>> ref;

	private CallGraph callGraph;

	private Map<CGNode, OrdinalSet<MethodReference>> ignored;

	private ArrayList<StageInterferenceInfo> interferenceInfos;

	public static PDGPartitionerChecker makePartitionChecker(ProgramDependenceGraph pdg, List<List<Integer>> selections) {
		PDGPartitionerChecker temp= new PDGPartitionerChecker(pdg);
		temp.convertSelectionToStages(selections);
		temp.mapNodesToStages();
		return temp;
	}

	private PDGPartitionerChecker(ProgramDependenceGraph pdg) {
		this.pdg= pdg;
	}

	public List<PipelineStage> convertSelectionToStages(List<List<Integer>> selections) {
		for (int index= 0; index < selections.size(); index++) {
			PipelineStage stage= PipelineStage.makePipelineStage(pdg, index, selections.get(index));
			stages.add(stage);
		}
		return stages;
	}

	public Map<PDGNode, Integer> mapNodesToStages() {
		for (int stage= 0; stage < stages.size(); stage++) {
			PipelineStage pipelineStage= stages.get(stage);
			List<PDGNode> selectedStatements= pipelineStage.getSelectedStatements();
			for (PDGNode node : selectedStatements) {
				node2stage.put(node, stage);
			}
		}
		return node2stage;
	}

	// For checking feasibility
	///////////////////////////

	// We take advantage of how WALA represents its dependencies using SSA form with Phi nodes 
	// to check for loop carried dependencies.
	// Basically if any of the stages (Stage1,...StageN) ever serve as an input dependence to 
	// the generator node then we have a loop carried dependency
	public boolean containsLoopCarriedDependency() {
		PipelineStage generator= getGenerator();
		List<DataDependence> inputDataDependences= generator.getInputDataDependences();
		for (DataDependence dependence : inputDataDependences) {
			PDGNode source= dependence.source;
			if (node2stage.get(source) != null) {
				return true; // We have this as one
			}
		}
		return false;
	}

	/**
	 * Sets up and starts the heap dependency analysis.
	 * 
	 * We intentionally separate heap analysis from scalar analysis because it is expensive. Thus we
	 * perform as much feasibility checks using intraprocedural checks first before turning this on.
	 * 
	 * @param callGraph
	 * @param pointerAnalysis
	 */
	public void computeHeapDependency(CallGraph callGraph, PointerAnalysis pointerAnalysis) {
		setupModRefInfrastructure(callGraph, pointerAnalysis);
		CGNode cgNode= getCurrentCGNode();
		for (PipelineStage stage : stages) {
			stage.computeHeapDependencies(cgNode, callGraph, pointerAnalysis, modref, mod, ref, ignored);

		}
	}

	// TODO: Investigate how we can get the correct context for the current CGNode. 
	//	Right now we are relying on the fact that there is a single context and using that
	private CGNode getCurrentCGNode() {
		MethodReference reference= pdg.getIr().getMethod().getReference();
		Set<CGNode> nodes= callGraph.getNodes(reference);

		Assertions.productionAssertion(nodes.size() == 1, "Expected a single corresponding CGNode, but got either 0 or more");

		CGNode node= nodes.iterator().next(); // Quick way to get first element of set with single entry since set doesn't implement get();
		return node;
	}

	private void setupModRefInfrastructure(CallGraph callGraph, PointerAnalysis pointerAnalysis) {
		this.callGraph= callGraph;
		this.modref= new JFlowModRef();
		this.mod= modref.computeMod(callGraph, pointerAnalysis);
		this.ref= modref.computeRef(callGraph, pointerAnalysis);
		this.ignored= modref.computeIgnoredCallee(callGraph, pointerAnalysis);
	}

	// For querying
	///////////////

	/**
	 * 
	 * @return Number of stages including the generator stage
	 */
	public int getNumberOfStages() {
		return stages.size();
	}

	/**
	 * Retrieve the different stages of the pipeline
	 * 
	 * @param stageNumber - Numbering starts with Stage1, Stage2, etc.
	 * @return The stage corresponding to the number
	 */
	public PipelineStage getStage(int stageNumber) {
		if (stageNumber <= 0 || stageNumber >= stages.size())
			throw new IllegalArgumentException(String.format("We don't have stage %d. Stages are from 0 to %d", stageNumber, stages.size() - 1));
		return stages.get(stageNumber);
	}

	/**
	 * 
	 * @return The generator stage
	 */
	public PipelineStage getGenerator() {
		return stages.get(0);
	}

	/*
	 * Returns a set of all the stages in this pipeline minus the parameter.
	 * XXX: This does not consider the generator.
	 */
	private Set<PipelineStage> getSetOfAllStagesExcluding(PipelineStage excluded) {
		Set<PipelineStage> otherStages= new HashSet<PipelineStage>();
		for (int stageNumber= 1; stageNumber < stages.size(); stageNumber++) {
			PipelineStage stage= stages.get(stageNumber);
			if (stage != excluded) {
				otherStages.add(stage);
			}
		}
		return otherStages;
	}

	public void checkInterference() {
		interferenceInfos= new ArrayList<StageInterferenceInfo>();
		for (int stage= 1; stage < stages.size(); stage++) {
			StageInterferenceInfo interferenceInfo= new StageInterferenceInfo(getStage(stage));
			interferenceInfo.checkInteference();
			interferenceInfos.add(interferenceInfo);
		}
	}

	public boolean hasInteference() {
		for (StageInterferenceInfo info : interferenceInfos) {
			if (info.hasInterference())
				return true;
		}
		return false;
	}

	// XXX: Make this human understandable
	public List<String> getInterferenceMessages() {
		List<String> interferenceMessages= new ArrayList<String>();

		for (StageInterferenceInfo info : interferenceInfos) {
			interferenceMessages.addAll(info.getInterferenceMessages());
		}

		return interferenceMessages;
	}

	public List<String> getIgnoredMethodCalls() {
		Set<MethodReference> ignoredMethods= new TreeSet<MethodReference>(new Comparator<MethodReference>() {

			@Override
			public int compare(MethodReference mRefLeft, MethodReference mRefRight) {
				return mRefLeft.toString().compareTo(mRefRight.toString());
			}
		});
		for (PipelineStage stage : stages) {
			ignoredMethods.addAll(stage.getIgnoreds());
		}

		List<String> ignored= new ArrayList<String>();
		for (MethodReference mRef : ignoredMethods) {
			ignored.add(mRef.toString());
		}
		return ignored;
	}

	/**
	 * This class checks for interferences between different stages.
	 * 
	 * It needs to know the set of all stages, S = {stage1, stage2, stage3, ..., stageN}. Then given
	 * a particular stage which is an element of S, call it stageK, it lists the interference that
	 * stageK might have with S \ {stageK}.
	 * 
	 * We define interference for stageK as a read of a PointerKey that is potentially modified by
	 * another stage. More concretely, we check if the elements of the REF set of stageK is part of
	 * the MOD set of the other stages.
	 * 
	 * We do this for each stage.
	 * 
	 * @author nchen
	 * 
	 */
	public class StageInterferenceInfo {
		private PipelineStage pipelineStage;

		Map<PipelineStage, Set<PointerKey>> interferences;

		public StageInterferenceInfo(PipelineStage pipelineStage) {
			this.pipelineStage= pipelineStage;
		}

		public void checkInteference() {
			initRecords();
			for (PipelineStage stage : interferences.keySet()) {
				Set<PointerKey> set= interferences.get(stage);
				set.retainAll(stage.getMods());
			}
		}

		public boolean hasInterference() {
			for (Set<PointerKey> pKeys : interferences.values()) {
				if (!pKeys.isEmpty())
					return true;
			}
			return false;
		}

		public List<String> getInterferenceMessages() {
			List<String> interferenceMessages= new ArrayList<String>();

			for (PipelineStage stage : interferences.keySet()) {
				Set<PointerKey> pKeys= interferences.get(stage);
				for (PointerKey pKey : pKeys) {
					interferenceMessages.add(String.format("Stage %d has possible interference with stage %d through %s", pipelineStage.getStageNumber(), stage.getStageNumber(), pKey));
				}
			}

			return interferenceMessages;
		}

		@Override
		public String toString() {
			StringBuilder sb= new StringBuilder();

			sb.append(String.format("Interference information for Stage %d%n", pipelineStage.getStageNumber()));
			sb.append(String.format("======%n"));
			sb.append(pipelineStage.getSelectedStatements());
			sb.append(String.format("%n%n"));

			for (PipelineStage stage : interferences.keySet()) {
				sb.append(String.format("With Stage %d%n", stage.getStageNumber()));
				sb.append(String.format("=====%n"));
				sb.append(interferences.get(stage));
				sb.append(String.format("%n%n"));
			}

			return sb.toString();
		}

		private void initRecords() {
			interferences= new HashMap<PipelineStage, Set<PointerKey>>();
			Set<PipelineStage> otherStages= getSetOfAllStagesExcluding(pipelineStage);
			for (PipelineStage stage : otherStages) {
				// We pre-populate the ref set with a copy so that we can do retainAll(), i.e., set intersection, which is destructive in Java
				interferences.put(stage, new HashSet<PointerKey>(pipelineStage.getRefs()));
			}
		}
	}
}
