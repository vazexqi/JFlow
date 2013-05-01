package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

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
	/**
	 * 
	 */
	private final PDGPartitionerChecker pdgPartitionerChecker;

	private PipelineStage pipelineStage;

	Map<PipelineStage, Set<PointerKey>> interferences;

	public StageInterferenceInfo(PDGPartitionerChecker pdgPartitionerChecker, PipelineStage pipelineStage) {
		this.pdgPartitionerChecker= pdgPartitionerChecker;
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
			Set<PointerKey> keys= interferences.get(stage);
			sb.append(PipelineStage.prettyPrint(keys, stage.getPointerAnalysis()));
		}

		return sb.toString();
	}

	private void initRecords() {
		interferences= new HashMap<PipelineStage, Set<PointerKey>>();
		Set<PipelineStage> otherStages= this.pdgPartitionerChecker.getSetOfAllStagesExcluding(pipelineStage);
		for (PipelineStage stage : otherStages) {
			// We pre-populate the ref set with a copy so that we can do retainAll(), i.e., set intersection, which is destructive in Java
			interferences.put(stage, new HashSet<PointerKey>(pipelineStage.getRefs()));
		}
	}
}