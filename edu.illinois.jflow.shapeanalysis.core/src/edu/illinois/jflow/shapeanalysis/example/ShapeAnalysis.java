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

public class ShapeAnalysis {

	class ShapeAnalysisDataflowSolver extends DataflowSolver<FictionalIR, StaticShapeGraph> {

		public ShapeAnalysisDataflowSolver(IKilldallFramework<FictionalIR, StaticShapeGraph> problem) {
			super(problem);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected StaticShapeGraph makeNodeVariable(FictionalIR n, boolean IN) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected StaticShapeGraph makeEdgeVariable(FictionalIR src, FictionalIR dst) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected StaticShapeGraph[] makeStmtRHS(int size) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	class ShapeAnalysisFramework extends BasicFramework<FictionalIR, StaticShapeGraph> {

		public ShapeAnalysisFramework(Graph<FictionalIR> cfg, ITransferFunctionProvider<FictionalIR, StaticShapeGraph> transferFunctionProvider) {
			super(cfg, transferFunctionProvider);
		}

	}

	class ShapeAnalysisTransferFunctionProvider implements ITransferFunctionProvider<FictionalIR, StaticShapeGraph> {

		@Override
		public UnaryOperator<StaticShapeGraph> getNodeTransferFunction(FictionalIR node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean hasNodeTransferFunctions() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public UnaryOperator<StaticShapeGraph> getEdgeTransferFunction(FictionalIR src, FictionalIR dst) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean hasEdgeTransferFunctions() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public AbstractMeetOperator<StaticShapeGraph> getMeetOperator() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public DataflowSolver solve() {
		// the framework describes the dataflow problem, in particular the underlying graph and the transfer functions
		ShapeAnalysisFramework framework= new ShapeAnalysisFramework(LinkedListNormalizedCFGFactory.createCFG(), new ShapeAnalysisTransferFunctionProvider());
		ShapeAnalysisDataflowSolver solver= new ShapeAnalysisDataflowSolver(framework);
		try {
			solver.solve(null);
		} catch (CancelException e) {
			assert false;
		}
		return solver;
	}
}
