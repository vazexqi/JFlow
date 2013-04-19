package edu.illinois.jflow.wala.modref;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionVisitor;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphTransitiveClosure;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.modref.DelegatingExtendedHeapModel;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.slicer.HeapExclusions;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.functions.Function;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.illinois.jflow.wala.pointeranalysis.AnalysisUtils;

public class JFlowModRef extends AstJavaModRef {

	/**
	 * For a call graph node, what ignored methods does it invoke, including it's callees
	 * transitively
	 */
	public Map<CGNode, OrdinalSet<MethodReference>> computeIgnoredCallee(CallGraph cg, PointerAnalysis pa) {
		return computeIgnoredCallee(cg, pa, null);
	}

	/**
	 * For a call graph node, what ignored methods does it invoke, including it's callees
	 * transitively
	 */
	public Map<CGNode, OrdinalSet<MethodReference>> computeIgnoredCallee(CallGraph cg, PointerAnalysis pa, HeapExclusions heapExclude) {
		if (cg == null) {
			throw new IllegalArgumentException("cg is null");
		}
		Map<CGNode, Collection<MethodReference>> ignored= scanForIgnoredCallees(cg, pa);
		return CallGraphTransitiveClosure.transitiveClosure(cg, ignored);
	}

	/**
	 * For a call graph node, what ignored methods does it invoke, <bf> NOT </bf> including it's
	 * callees transitively
	 */
	private Map<CGNode, Collection<MethodReference>> scanForIgnoredCallees(CallGraph cg, final PointerAnalysis pa) {

		return CallGraphTransitiveClosure.collectNodeResults(cg, new Function<CGNode, Collection<MethodReference>>() {

			public Collection<MethodReference> apply(CGNode n) {
				return scanNodeForIgnoredCallees(n, pa);
			}
		});
	}

	/**
	 * For a call graph node, what ignored methods does it invoke, <bf> NOT </bf> including it's
	 * callees transitively
	 */
	private Collection<MethodReference> scanNodeForIgnoredCallees(final CGNode n, final PointerAnalysis pa) {
		Collection<MethodReference> result= HashSetFactory.make();
		final ExtendedHeapModel h= new DelegatingExtendedHeapModel(pa.getHeapModel());
		JFlowIgnoredCalleVisitor v= makeIgnoredCalleeVisitor(n, result, pa, h);
		IR ir= n.getIR();
		if (ir != null) {
			for (Iterator<SSAInstruction> it= ir.iterateNormalInstructions(); it.hasNext();) {
				it.next().visit(v);
			}
		}
		return result;
	}

	protected static class JFlowIgnoredCalleVisitor implements AstJavaInstructionVisitor {

		private final CGNode n;

		private final Collection<MethodReference> result;

		private final PointerAnalysis pa;

		private final ExtendedHeapModel h;

		public JFlowIgnoredCalleVisitor(CGNode n, Collection<MethodReference> result, ExtendedHeapModel h, PointerAnalysis pa) {
			this.n= n;
			this.result= result;
			this.pa= pa;
			this.h= h;
		}

		// Collect all the methods that we know nothing about but we invoke. This will be a list of warnings
		@Override
		public void visitInvoke(SSAInvokeInstruction instruction) {
			MethodReference declaredTarget= instruction.getDeclaredTarget();
			if (AnalysisUtils.isLibraryClass(declaredTarget.getDeclaringClass())) {
				result.add(declaredTarget);
			}
		}

		// The rest are empty since we are not doing anything

		@Override
		public void visitAstLexicalRead(AstLexicalRead instruction) {
		}

		@Override
		public void visitAstLexicalWrite(AstLexicalWrite instruction) {
		}

		@Override
		public void visitAstGlobalRead(AstGlobalRead instruction) {
		}

		@Override
		public void visitAstGlobalWrite(AstGlobalWrite instruction) {
		}

		@Override
		public void visitAssert(AstAssertInstruction instruction) {
		}

		@Override
		public void visitEachElementGet(EachElementGetInstruction inst) {
		}

		@Override
		public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
		}

		@Override
		public void visitIsDefined(AstIsDefinedInstruction inst) {
		}

		@Override
		public void visitEcho(AstEchoInstruction inst) {
		}

		@Override
		public void visitGoto(SSAGotoInstruction instruction) {
		}

		@Override
		public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		}

		@Override
		public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		}

		@Override
		public void visitBinaryOp(SSABinaryOpInstruction instruction) {
		}

		@Override
		public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
		}

		@Override
		public void visitConversion(SSAConversionInstruction instruction) {
		}

		@Override
		public void visitComparison(SSAComparisonInstruction instruction) {
		}

		@Override
		public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
		}

		@Override
		public void visitSwitch(SSASwitchInstruction instruction) {
		}

		@Override
		public void visitReturn(SSAReturnInstruction instruction) {
		}

		@Override
		public void visitGet(SSAGetInstruction instruction) {
		}

		@Override
		public void visitPut(SSAPutInstruction instruction) {
		}


		@Override
		public void visitNew(SSANewInstruction instruction) {
		}

		@Override
		public void visitArrayLength(SSAArrayLengthInstruction instruction) {
		}

		@Override
		public void visitThrow(SSAThrowInstruction instruction) {
		}

		@Override
		public void visitMonitor(SSAMonitorInstruction instruction) {
		}

		@Override
		public void visitCheckCast(SSACheckCastInstruction instruction) {
		}

		@Override
		public void visitInstanceof(SSAInstanceofInstruction instruction) {
		}

		@Override
		public void visitPhi(SSAPhiInstruction instruction) {
		}

		@Override
		public void visitPi(SSAPiInstruction instruction) {
		}

		@Override
		public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
		}

		@Override
		public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
		}

		@Override
		public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
		}

		@Override
		public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
		}

	}

	protected JFlowIgnoredCalleVisitor makeIgnoredCalleeVisitor(CGNode n, Collection<MethodReference> result, PointerAnalysis pa, ExtendedHeapModel h) {
		return makeIgnoredCalleeVisitor(n, result, pa, h, false);
	}

	protected JFlowIgnoredCalleVisitor makeIgnoredCalleeVisitor(CGNode n, Collection<MethodReference> result, PointerAnalysis pa, ExtendedHeapModel h, boolean ignoreAllocHeapDefs) {
		return new JFlowIgnoredCalleVisitor(n, result, h, pa);
	}
}
