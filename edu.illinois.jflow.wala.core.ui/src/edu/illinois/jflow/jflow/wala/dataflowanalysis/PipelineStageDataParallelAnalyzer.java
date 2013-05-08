package edu.illinois.jflow.jflow.wala.dataflowanalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

import edu.illinois.jflow.wala.pointeranalysis.JFlowCustomContextSelector;
import edu.illinois.jflow.wala.pointeranalysis.ReceiverString;
import edu.illinois.jflow.wala.pointeranalysis.ReceiverStringContext;

public class PipelineStageDataParallelAnalyzer {
	private PipelineStage stage; // Represents the stage that we are analyzing

	private PDGPartitionerChecker checker; // Use this to get information about prior stage

	private List<NewSiteReference> newSiteRefsInStage= new ArrayList<NewSiteReference>(); // Use to keep track of things allocated here

	private Set<PointerKey> mods;

	public PipelineStageDataParallelAnalyzer(PDGPartitionerChecker checker, PipelineStage stage) {
		this.checker= checker;
		this.stage= stage;
	}

	public void checkDataParallelizable() {
		initAllocatedObjects();
		mods= stage.getMods();
		Set<PointerKey> snapshot= new HashSet(mods); // We need a snapshot since we are removing things as we iterate
		for (PointerKey key : snapshot) {
			checkPointerKeyAllocatedInStage(key);
		}
	}

	public boolean isDataParallelizable() {
		return mods.isEmpty();
	}

	/**
	 * Handles each type of pointer key. This is the quick n' dirty way of doing if we don't want to
	 * modify the core Wala classes.
	 * 
	 * @param key
	 */
	public void checkPointerKeyAllocatedInStage(PointerKey key) {
		if (key instanceof InstanceFieldPointerKey) {
			InstanceFieldPointerKey instanceFieldPointerKey= (InstanceFieldPointerKey)key;
			handle(instanceFieldPointerKey);
		} else if (key instanceof StaticFieldKey) {
			StaticFieldKey staticFieldKey= (StaticFieldKey)key;
			handle(staticFieldKey);
		} else {
			handle(key);
		}
	}

	private void handle(InstanceFieldPointerKey instanceFieldPointerKey) {
		if (instanceFieldPointerKey instanceof InstanceFieldKey) {
			InstanceFieldKey a= (InstanceFieldKey)instanceFieldPointerKey;
			IField field= a.getField();
		}
		InstanceKey instanceKey= instanceFieldPointerKey.getInstanceKey();
		if (instanceKey instanceof AllocationSiteInNode) {
			removePointerKeyIfAllocatedByCurrentStage(instanceFieldPointerKey, instanceKey); // Remove from top level
			AllocationSiteInNode allocNode= (AllocationSiteInNode)instanceKey;
			Context context= allocNode.getNode().getContext();
			if (context instanceof ReceiverStringContext) {
				ReceiverStringContext receiverContext= (ReceiverStringContext)context;
				ReceiverString contextItem= (ReceiverString)receiverContext.get(JFlowCustomContextSelector.RECEIVER_STRING);
				InstanceKey[] instances= contextItem.getInstances();
				for (InstanceKey instance : instances) {
					removePointerKeyIfAllocatedByCurrentStage(instanceFieldPointerKey, instance);
				}
			}
		}

	}

	private void removePointerKeyIfAllocatedByCurrentStage(InstanceFieldPointerKey instanceFieldPointerKey, InstanceKey instanceKey) {
		if (instanceKey instanceof AllocationSiteInNode) {
			AllocationSiteInNode allocNode= (AllocationSiteInNode)instanceKey;
			if (allocNode.getNode().equals(stage.getCgNode())) {
				NewSiteReference site= allocNode.getSite();
				if (newSiteRefsInStage.contains(site)) {
					mods.remove(instanceFieldPointerKey);
				}
			}
		}
	}

	private void handle(PointerKey key) {
		System.err.println("Unhandled key: " + key);
	}

	// The fact that we modify a static field means that is definitely not data-parallelizable
	private void handle(StaticFieldKey staticFieldKey) {
		// Do nothing for now
	}

	private void initAllocatedObjects() {
		getLocallyAllocatedObjects();
		getObjectsFromPreviousStage();
	}

	private void getObjectsFromPreviousStage() {
		// Filter out the pointers that are passed between stages
		DelegatingExtendedHeapModel heapModel= stage.getHeapModel();
		CGNode cgNode= stage.getCgNode();

		List<DataDependence> dataDependencies= new ArrayList<DataDependence>();
		PipelineStage previousStage= checker.getStage(stage.getStageNumber() - 1);
		dataDependencies.addAll(previousStage.getOutputDataDependences());

		for (DataDependence dDep : dataDependencies) {
			int SSAVariableNumber= dDep.getSSAVariableNumber();
			if (SSAVariableNumber != DataDependence.DEFAULT_SSAVARIABLENUMBER) {
				PointerKey ref= heapModel.getPointerKeyForLocal(cgNode, SSAVariableNumber);
				if (ref != null) {
					if (ref instanceof InstanceFieldPointerKey) {
						InstanceFieldPointerKey instanceFieldPointerKey= (InstanceFieldPointerKey)ref;
						InstanceKey instanceKey= instanceFieldPointerKey.getInstanceKey();
						Iterator<Pair<CGNode, NewSiteReference>> creationSites= instanceKey.getCreationSites(stage.getCallGraph());
						for (Pair<CGNode, NewSiteReference> pair : Iterator2Iterable.make(creationSites)) {
							if (pair.fst.equals(stage.getCgNode())) {
								newSiteRefsInStage.add(pair.snd);
							}
						}
					}
				}
			}
		}
	}

	private void getLocallyAllocatedObjects() {
		List<PDGNode> selectedStatements= stage.getSelectedStatements();
		for (PDGNode node : selectedStatements) {
			if (node instanceof Statement) {
				Statement stmt= (Statement)node;
				List<SSAInstruction> ssaInstructions= stmt.retrieveAllSSAInstructions();
				for (SSAInstruction ssaInstruction : ssaInstructions) {
					if (ssaInstruction instanceof SSANewInstruction) { // We are only interested in new instructions
						SSANewInstruction newInstruction= (SSANewInstruction)ssaInstruction;
						newSiteRefsInStage.add(newInstruction.getNewSite());
					}
				}
			} else {
				// This should not happen since we are dealing only with statements
				Assertions.UNREACHABLE("Found something that was not a Statement node.");
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb= new StringBuilder();
		sb.append(String.format("Stage %d%n", stage.getStageNumber()));
		sb.append(String.format("====%n"));
		for (PointerKey pKey : mods) {
			Set<Statement> modifyingStatements= stage.modifyingStatements(pKey);
			for (Statement statement : modifyingStatements) {
				String currentStageSourceCode= statement.getSourceCode().isEmpty() ? statement.toString() : statement.getSourceCode();
				sb.append(String.format("%s modifies %s", currentStageSourceCode, PointerKeyPrettyPrinter.prettyPrint(pKey)));
			}
		}
		return sb.toString();
	}
}
