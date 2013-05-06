package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;

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

	public void checkInterference() {
		initRecords();
		for (PipelineStage stage : interferences.keySet()) {
			Set<PointerKey> set= interferences.get(stage);
			set.retainAll(stage.getMods());
		}

		// Filter out the pointers that are passed between stages
		DelegatingExtendedHeapModel heapModel= pipelineStage.getHeapModel();
		CGNode cgNode= pipelineStage.getCgNode();

		List<DataDependence> dataDependencies= new ArrayList<DataDependence>();
		// XXX: Improve this
		// Gather all the data that could be produced from any of the other stages
		// This is a shortcut (it doesn't consider flow). However, it is safe because
		// flow is implicitly considered through the use of SSAVariables and also the fact
		// that we only allow straight-line code. A proper, but more expensive, way
		// would be to consider each combination of possible producer-consumer pairs
		dataDependencies.addAll(pipelineStage.getOutputDataDependences());
		for (PipelineStage stage : interferences.keySet()) {
			dataDependencies.addAll(stage.getOutputDataDependences());
		}

		for (DataDependence dDep : dataDependencies) {
			int SSAVariableNumber= dDep.getSSAVariableNumber();
			if (SSAVariableNumber != DataDependence.DEFAULT_SSAVARIABLENUMBER) {
				PointerKey ref= heapModel.getPointerKeyForLocal(cgNode, SSAVariableNumber);
				if (ref != null) {
					// Remove this pointer key from the interference of sets
					for (PipelineStage stage : interferences.keySet()) {
						Set<PointerKey> set= interferences.get(stage);
						pruneTransferredObject(ref, set);
					}
				}
			}
		}
	}

	// We can only transfer non-static objects - all static objects are otherwise shared
	private void pruneTransferredObject(PointerKey ref, Set<PointerKey> set) {
		PointerAnalysis pointerAnalysis= pipelineStage.getPointerAnalysis();
		for (InstanceKey transferredObject : pointerAnalysis.getPointsToSet(ref)) {
			Set<PointerKey> snapshot= new HashSet<PointerKey>(set); // We are going to remove things so let's snapshot the contents and iterate through that
			for (PointerKey pKey : snapshot) {
				if (pKey instanceof InstanceFieldPointerKey) {
					InstanceFieldPointerKey instanceFieldPointerKey= (InstanceFieldPointerKey)pKey;
					InstanceKey instanceKey= instanceFieldPointerKey.getInstanceKey();
					if (instanceKey.equals(transferredObject)) {
						set.remove(pKey);
					}
				}
			}
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
