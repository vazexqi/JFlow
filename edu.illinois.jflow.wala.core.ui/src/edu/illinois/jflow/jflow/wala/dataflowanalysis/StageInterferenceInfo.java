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
 * It needs to know the set of all stages, S = {stage1, stage2, stage3, ..., stageN}. Then given a
 * particular stage which is an element of S, call it stageK, it lists the interference that stageK
 * might have with S \ {stageK}.
 * 
 * We define interference for stageK as a read of a PointerKey that is potentially modified by
 * another stage. More concretely, we check if the elements of the REF set of stageK is part of the
 * MOD set of the other stages.
 * 
 * We do this for each stage.
 * 
 * @author nchen
 * 
 */
public class StageInterferenceInfo {
	private final PDGPartitionerChecker pdgPartitionerChecker;

	private final PipelineStage pipelineStage;

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

		for (PipelineStage otherStage : interferences.keySet()) {
			Set<PointerKey> pKeys= interferences.get(otherStage);
			for (PointerKey pKey : pKeys) {
				StringBuilder sb= new StringBuilder();

				int stageNumber= pipelineStage.getStageNumber();
				int otherStageNumber= otherStage.getStageNumber();

				// We could have a read-write dependency and/or a write-write dependency
				// Be sure to add BOTH

				Set<Statement> thisStageRefStatements= pipelineStage.referringStatements(pKey);
				Set<Statement> thisStageModStatements= pipelineStage.modifyingStatements(pKey);

				Set<Statement> otherStageModStatements= otherStage.modifyingStatements(pKey);
				Statement otherStageFirstMod= otherStageModStatements.iterator().next();
				String otherStageWriteStmt= otherStageFirstMod.getSourceCode().isEmpty() ? otherStageFirstMod.toString() : otherStageFirstMod.getSourceCode();

				// Add read-write dependency (if any)
				if (thisStageRefStatements != null && !thisStageRefStatements.isEmpty()) {
					Statement firstRef= thisStageRefStatements.iterator().next();
					String readStmt= firstRef.getSourceCode().isEmpty() ? firstRef.toString() : firstRef.getSourceCode();

					sb.append(String.format("Possible data race between stage #%d and stage #%d through %s", stageNumber, otherStageNumber, PointerKeyPrettyPrinter.prettyPrint(pKey)));
					sb.append(String.format("Stage #%d reads the value through %s.%n", stageNumber, readStmt));
					sb.append(String.format("Stage #%d modifies the value through %s.%n", otherStageNumber, otherStageWriteStmt));
					interferenceMessages.add(sb.toString());
				}

				// Add write-write dependency (if any)
				if (thisStageModStatements != null && !thisStageModStatements.isEmpty()) {
					Statement firstMod= thisStageModStatements.iterator().next();
					String writeStmt= firstMod.getSourceCode().isEmpty() ? firstMod.toString() : firstMod.getSourceCode();

					sb.append(String.format("Possible data race between stage #%d and stage #%d through %s", stageNumber, otherStageNumber, PointerKeyPrettyPrinter.prettyPrint(pKey)));
					sb.append(String.format("Stage #%d modifies the value through %s.%n", stageNumber, writeStmt));
					sb.append(String.format("Stage #%d modifies the value through %s.%n", otherStageNumber, otherStageWriteStmt));
					interferenceMessages.add(sb.toString());
				}
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
		Set<PipelineStage> otherStages= pdgPartitionerChecker.getSetOfAllStagesExcluding(pipelineStage);
		for (PipelineStage stage : otherStages) {
			// We pre-populate the set with a copy of (Ref U Mod) so that we can do retainAll(), i.e., set intersection, which is destructive in Java
			Set<PointerKey> refs= pipelineStage.getRefs();
			refs.addAll(pipelineStage.getMods());
			interferences.put(stage, new HashSet<PointerKey>(refs));
		}
	}
}
