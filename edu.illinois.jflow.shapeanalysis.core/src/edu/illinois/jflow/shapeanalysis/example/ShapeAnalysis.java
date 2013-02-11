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
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.SelectorEdge;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.ShapeNode;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.VariableEdge;

/**
 * A "somewhat" specialized solver to solve the dataflow problem set up in Figure 5 of the paper
 * "Solving Shape-Analysis Problems in Languages with Destructive Updating".
 * 
 * @author nchen
 * 
 */
public class ShapeAnalysis {

	static StaticShapeGraph initialState() {
		StaticShapeGraph initialGraph= new StaticShapeGraph();

		// This is the initial setup that we want
		//
		//        -----              -----
		// x --> |  x  | -- cdr --> | phi | ----
		//        -----              -----      | cdr
		//                             ^ -------
		//
		PointerVariable x= new PointerVariable("x");
		ShapeNode xNode= new ShapeNode(x);
		ShapeNode phiNode= ShapeNode.getPhiNode();

		VariableEdge xPointer= new VariableEdge(x, xNode);
		initialGraph.addVariableEdge(xPointer);

		SelectorEdge xToPhi= new SelectorEdge(xNode, new Selector("cdr"), phiNode);
		SelectorEdge phiToPhi= new SelectorEdge(phiNode, new Selector("cdr"), phiNode);
		initialGraph.addSelectorEdge(xToPhi);
		initialGraph.addSelectorEdge(phiToPhi);

		return initialGraph;
	}

	static class ShapeAnalysisFramework extends BasicFramework<FictionalIR, StaticShapeGraph> {

		public ShapeAnalysisFramework(Graph<FictionalIR> cfg, ITransferFunctionProvider<FictionalIR, StaticShapeGraph> transferFunctionProvider) {
			super(cfg, transferFunctionProvider);
		}

	}

	static class ShapeAnalysisTransferFunctionProvider implements ITransferFunctionProvider<FictionalIR, StaticShapeGraph> {

		@Override
		public UnaryOperator<StaticShapeGraph> getNodeTransferFunction(FictionalIR node) {
			return node.getTransferFunction();
		}

		@Override
		public boolean hasNodeTransferFunctions() {
			return true;
		}

		@Override
		public UnaryOperator<StaticShapeGraph> getEdgeTransferFunction(FictionalIR src, FictionalIR dst) {
			throw new UnsupportedOperationException("There are no edge functions and thus there shouldn't be a call to this method");
		}

		@Override
		public boolean hasEdgeTransferFunctions() {
			return false;
		}

		@Override
		public AbstractMeetOperator<StaticShapeGraph> getMeetOperator() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	static class ShapeAnalysisDataflowSolver extends DataflowSolver<FictionalIR, StaticShapeGraph> {

		public ShapeAnalysisDataflowSolver(IKilldallFramework<FictionalIR, StaticShapeGraph> problem) {
			super(problem);
		}

		@Override
		protected StaticShapeGraph makeNodeVariable(FictionalIR n, boolean IN) {
			// TODO Check if this is the right place to put the initial values
			// We are going to follow Fig 5. of the paper and initialize the case where the linked list has several variables
			return ShapeAnalysis.initialState();
		}

		@Override
		protected StaticShapeGraph makeEdgeVariable(FictionalIR src, FictionalIR dst) {
			// We are not going to use any edgeTransferFunction so there is no need to create edge variables
			return null;
		}

		@Override
		protected StaticShapeGraph[] makeStmtRHS(int size) {
			// TODO Auto-generated method stub
			return null;

		}

		public DataflowSolver solve() {
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
		solver.solve();
	}
}
