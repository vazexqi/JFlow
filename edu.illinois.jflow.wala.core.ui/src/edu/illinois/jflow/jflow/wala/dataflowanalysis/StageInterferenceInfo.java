package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.util.collections.Pair;

import edu.illinois.jflow.wala.pointeranalysis.JFlowCustomContextSelector;
import edu.illinois.jflow.wala.pointeranalysis.ReceiverString;
import edu.illinois.jflow.wala.pointeranalysis.ReceiverStringContext;

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
		// This is a shortcut that is safe because
		// flow is implicitly considered through the use of SSAVariables and also the fact
		// that we only allow linear pipelines.
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
	// Also transfer all the other objects that it creates transitively
	private void pruneTransferredObject(PointerKey root, Set<PointerKey> set) {
		PointerAnalysis pointerAnalysis= pipelineStage.getPointerAnalysis();
		for (InstanceKey transferredObject : pointerAnalysis.getPointsToSet(root)) {
			Set<PointerKey> snapshot= new HashSet<PointerKey>(set); // We are going to remove things so let's snapshot the contents and iterate through that
			for (PointerKey pKey : snapshot) {
				if (pKey instanceof InstanceFieldPointerKey) {
					InstanceFieldPointerKey instanceFieldPointerKey= (InstanceFieldPointerKey)pKey;
					InstanceKey instanceKey= instanceFieldPointerKey.getInstanceKey();

					// Simple case of equality
					if (instanceKey.equals(transferredObject)) {
						set.remove(pKey);
					} else if (instanceKey instanceof AllocationSiteInNode) {
						AllocationSiteInNode allocNode= (AllocationSiteInNode)instanceKey;
						Context context= allocNode.getNode().getContext();
						if (context instanceof ReceiverStringContext) {
							ReceiverStringContext receiverContext= (ReceiverStringContext)context;
							ReceiverString contextItem= (ReceiverString)receiverContext.get(JFlowCustomContextSelector.RECEIVER_STRING);
							InstanceKey[] instances= contextItem.getInstances();
							for (InstanceKey instance : instances) {
								if (instance.equals(transferredObject))
									set.remove(pKey);
							}
						}
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

	Map<Pair<Statement, Statement>, InterferencePair> interferingPairQuickLookUp= new HashMap<Pair<Statement, Statement>, InterferencePair>();

	class InterferencePair {
		public static final int THRESHOLD= 10; // Number of warnings to display before alerting the user

		PipelineStage otherStage;

		Statement currentStageStmt;

		Statement otherStageStmt;

		List<PointerKey> interferringAccesses= new ArrayList<PointerKey>();

		public InterferencePair(PipelineStage otherStage, Statement currentStageStmt, Statement otherStageStmt) {
			this.otherStage= otherStage;
			this.currentStageStmt= currentStageStmt;
			this.otherStageStmt= otherStageStmt;
		}

		public void addPointerKey(PointerKey pKey) {
			interferringAccesses.add(pKey);
		}

		@Override
		public String toString() {
			StringBuilder sb= new StringBuilder();

			String template= "[Stage#%d]%s %n [Stage#%d]%s %n concurrently access:%n";
			String currentStageSourceCode= currentStageStmt.getSourceCode().isEmpty() ? currentStageStmt.toString() : currentStageStmt.getSourceCode();
			String otherStageSourcecode= otherStageStmt.getSourceCode().isEmpty() ? otherStageStmt.toString() : otherStageStmt.getSourceCode();
			sb.append(String.format(template, pipelineStage.getStageNumber(), currentStageSourceCode, otherStage.getStageNumber(), otherStageSourcecode));

			for (int index= 0; index < Math.min(interferringAccesses.size(), THRESHOLD); index++) {
				PointerKey pKey= interferringAccesses.get(index);
				sb.append(String.format("%s", PointerKeyPrettyPrinter.prettyPrint(pKey)));
			}

			if (interferringAccesses.size() > THRESHOLD) {
				sb.append(String.format("....%n"));
				sb.append(String.format("Suppressing more than %d concurrent accesses reported. Please inspect manually since these are likely to be spurious.%n", THRESHOLD));
			}

			return sb.toString();
		}
	}

	public void constructInterferenceInformation() {
		for (PipelineStage otherStage : interferences.keySet()) {
			Set<PointerKey> pKeys= interferences.get(otherStage);
			for (PointerKey pKey : pKeys) {
				Set<Statement> thisStageRefStatements= pipelineStage.referringStatements(pKey);
				Set<Statement> thisStageModStatements= pipelineStage.modifyingStatements(pKey);

				Set<Statement> otherStageModStatements= otherStage.modifyingStatements(pKey);
				Statement otherStageFirstMod= otherStageModStatements.iterator().next();

				// Add read-write dependency (if any)
				if (thisStageRefStatements != null && !thisStageRefStatements.isEmpty()) {
					Statement firstRef= thisStageRefStatements.iterator().next();
					addNewInterferencePairRecord(otherStage, firstRef, otherStageFirstMod, pKey);
				}

				// Add write-write dependency (if any)
				if (thisStageModStatements != null && !thisStageModStatements.isEmpty()) {
					Statement firstMod= thisStageModStatements.iterator().next();
					addNewInterferencePairRecord(otherStage, firstMod, otherStageFirstMod, pKey);
				}
			}
		}
	}

	public void addNewInterferencePairRecord(PipelineStage otherStage, Statement currentStageStmt, Statement otherStageStmt, PointerKey pKey) {
		Pair<Statement, Statement> pair= Pair.make(currentStageStmt, otherStageStmt);
		InterferencePair interferencePair= interferingPairQuickLookUp.get(pair);
		if (interferencePair == null) {
			InterferencePair newPair= new InterferencePair(otherStage, currentStageStmt, otherStageStmt);
			interferingPairQuickLookUp.put(pair, newPair);
			interferencePair= newPair;
		}
		interferencePair.addPointerKey(pKey);
	}

	public List<String> getInterferenceMessages() {
		List<String> interferenceMessages= new ArrayList<String>();
		constructInterferenceInformation();

		Map<PipelineStage, List<InterferencePair>> stage2InterferencePair= new HashMap<PipelineStage, List<InterferencePair>>();

		// Go through and sort the accesses
		for (InterferencePair pair : interferingPairQuickLookUp.values()) {
			PipelineStage otherStage= pair.otherStage;
			List<InterferencePair> list= stage2InterferencePair.get(otherStage);
			if (list == null) {
				list= new ArrayList<InterferencePair>();
				stage2InterferencePair.put(otherStage, list);
			}
			list.add(pair);
		}

		for (PipelineStage stage : stage2InterferencePair.keySet()) {
			List<InterferencePair> list= stage2InterferencePair.get(stage);
			for (InterferencePair pair : list) {
				interferenceMessages.add(pair.toString());
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
