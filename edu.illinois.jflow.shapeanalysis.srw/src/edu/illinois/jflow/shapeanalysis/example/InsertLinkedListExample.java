package edu.illinois.jflow.shapeanalysis.example;

import com.ibm.wala.dataflow.graph.DataflowSolver;

import edu.illinois.jflow.shapeanalysis.example.ShapeAnalysis.ShapeAnalysisDataflowSolver;
import edu.illinois.jflow.shapeanalysis.example.ShapeAnalysis.ShapeAnalysisFramework;
import edu.illinois.jflow.shapeanalysis.example.ShapeAnalysis.ShapeAnalysisTransferFunctionProvider;
import edu.illinois.jflow.shapeanalysis.example.ir.FictionalIR;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;

public class InsertLinkedListExample {
	public static void main(String[] args) {
		ShapeAnalysisDataflowSolver solver= new ShapeAnalysisDataflowSolver(new ShapeAnalysisFramework(InsertLinkedListNormalizedCFGFactory.createCFG(), new ShapeAnalysisTransferFunctionProvider()));
		DataflowSolver<FictionalIR<StaticShapeGraph>, StaticShapeGraph> solution= solver.solve();

		FictionalIR<StaticShapeGraph>[] ir= InsertLinkedListNormalizedCFGFactory.instr;
		for (int i= 0; i < ir.length; i++) {
			System.out.println("[" + i + "] " + ir[i]);
			System.out.println("---");

			System.out.println("(IN)");
			StaticShapeGraph in= solution.getIn(ir[i]);
			System.out.println(in);

			System.out.println("(OUT)");
			StaticShapeGraph out= solution.getOut(ir[i]);
			System.out.println(out);
		}
	}
}
