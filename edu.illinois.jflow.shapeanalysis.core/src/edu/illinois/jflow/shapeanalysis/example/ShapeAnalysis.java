package edu.illinois.jflow.shapeanalysis.example;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BasicFramework;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.dataflow.graph.IKilldallFramework;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.Graph;

import edu.illinois.jflow.shapeanalysis.example.ir.FictionalIR;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;

/**
 * A "somewhat" specialized solver to solve the dataflow problem set up in Figure 5 of the paper
 * "Solving Shape-Analysis Problems in Languages with Destructive Updating".
 * 
 * @author nchen
 * 
 */
public class ShapeAnalysis {

	static class ShapeAnalysisFramework extends BasicFramework<FictionalIR<StaticShapeGraph>, StaticShapeGraph> {

		public ShapeAnalysisFramework(Graph<FictionalIR<StaticShapeGraph>> cfg, ITransferFunctionProvider<FictionalIR<StaticShapeGraph>, StaticShapeGraph> transferFunctionProvider) {
			super(cfg, transferFunctionProvider);
		}

	}

	static class ShapeAnalysisTransferFunctionProvider implements ITransferFunctionProvider<FictionalIR<StaticShapeGraph>, StaticShapeGraph> {

		@Override
		public UnaryOperator<StaticShapeGraph> getNodeTransferFunction(FictionalIR<StaticShapeGraph> node) {
			return node.getTransferFunction();
		}

		@Override
		public boolean hasNodeTransferFunctions() {
			return true;
		}

		@Override
		public UnaryOperator<StaticShapeGraph> getEdgeTransferFunction(FictionalIR<StaticShapeGraph> src, FictionalIR<StaticShapeGraph> dst) {
			throw new UnsupportedOperationException("There are no edge functions and thus there shouldn't be a call to this method");
		}

		@Override
		public boolean hasEdgeTransferFunctions() {
			return false;
		}

		@Override
		public AbstractMeetOperator<StaticShapeGraph> getMeetOperator() {
			return SSGMeetOperator.instance();
		}
	}

	static class ShapeAnalysisDataflowSolver extends DataflowSolver<FictionalIR<StaticShapeGraph>, StaticShapeGraph> {

		public ShapeAnalysisDataflowSolver(IKilldallFramework<FictionalIR<StaticShapeGraph>, StaticShapeGraph> problem) {
			super(problem);
		}

		@Override
		protected StaticShapeGraph makeNodeVariable(FictionalIR<StaticShapeGraph> n, boolean IN) {
			if (n.hasInitialValue())
				return n.getInitialValue();
			return new StaticShapeGraph();
		}

		@Override
		protected StaticShapeGraph makeEdgeVariable(FictionalIR<StaticShapeGraph> src, FictionalIR<StaticShapeGraph> dst) {
			// We are not going to use any edgeTransferFunction so there is no need to create edge variables
			throw new UnsupportedOperationException("There are no edge functions and thus there shouldn't be a call to this method");
		}

		@Override
		protected StaticShapeGraph[] makeStmtRHS(int size) {
			return new StaticShapeGraph[size];
		}

		public DataflowSolver<FictionalIR<StaticShapeGraph>, StaticShapeGraph> solve() {
			try {
				solve(null);
			} catch (CancelException e) {
				assert false;
			}
			return this;
		}
	}

	public static void main(String[] args) {
		ShapeAnalysisDataflowSolver solver= new ShapeAnalysisDataflowSolver(new ShapeAnalysisFramework(LinkedListNormalizedCFGFactory.createCFG(), new ShapeAnalysisTransferFunctionProvider()));
		DataflowSolver<FictionalIR<StaticShapeGraph>, StaticShapeGraph> solution= solver.solve();

		FictionalIR<StaticShapeGraph>[] ir= LinkedListNormalizedCFGFactory.instr;
		for (int i= 0; i < ir.length; i++) {
			System.out.println("[" + i + "] " + ir[i]);
			System.out.println("---");

			System.out.println("(IN)");
			StaticShapeGraph in= solution.getIn(ir[i]);
			System.out.println(in);

			System.out.println("OUT");
			StaticShapeGraph out= solution.getOut(ir[i]);
			System.out.println(out);
		}
	}
}
