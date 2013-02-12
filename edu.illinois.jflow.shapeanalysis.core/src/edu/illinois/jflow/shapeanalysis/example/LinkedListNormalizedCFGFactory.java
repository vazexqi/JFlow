package edu.illinois.jflow.shapeanalysis.example;

import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

import edu.illinois.jflow.shapeanalysis.example.ir.AssignInstruction;
import edu.illinois.jflow.shapeanalysis.example.ir.AssignNilInstruction;
import edu.illinois.jflow.shapeanalysis.example.ir.DontCareInstruction;
import edu.illinois.jflow.shapeanalysis.example.ir.FictionalIR;
import edu.illinois.jflow.shapeanalysis.example.ir.GetSelectorInstruction;
import edu.illinois.jflow.shapeanalysis.example.ir.PutNilInstruction;
import edu.illinois.jflow.shapeanalysis.example.ir.PutSelectorInstruction;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.PointerVariable;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.Selector;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.SelectorEdge;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.ShapeNode;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;
import edu.illinois.jflow.shapenalaysis.shapegraph.structures.VariableEdge;

/**
 * Returns a graph with FictionalIR as the nodes. The edges between the nodes represent control
 * flow. This is basically a hard coded vesrion of the CFG from Figure 5 of the paper
 * "Solving Shape-Analysis Problems in Languages with Destructive Updating".
 * 
 * @author nchen
 * 
 */
public class LinkedListNormalizedCFGFactory {

	public static FictionalIR<StaticShapeGraph>[] instr;

	@SuppressWarnings("unchecked")
	static Graph<FictionalIR<StaticShapeGraph>> createCFG() {
		Graph<FictionalIR<StaticShapeGraph>> graph= SlowSparseNumberedGraph.make();

		instr= new FictionalIR[15];

		instr[0]= new AssignNilInstruction(new PointerVariable("y")); // y := nil
		instr[1]= new DontCareInstruction(); // x != nil
		instr[2]= new AssignNilInstruction(new PointerVariable("t")); // t := nil
		instr[3]= new AssignInstruction(new PointerVariable("t"), new PointerVariable("y")); // t := y
		instr[4]= new AssignNilInstruction(new PointerVariable("y")); // y := nil
		instr[5]= new AssignInstruction(new PointerVariable("y"), new PointerVariable("x")); // y := x
		instr[6]= new AssignNilInstruction(new PointerVariable("t1")); // t1 := nil
		instr[7]= new GetSelectorInstruction(new PointerVariable("t1"), new Selector("cdr"), new PointerVariable("x")); // t1 := x.cdr
		instr[8]= new AssignNilInstruction(new PointerVariable("x")); // x := nil
		instr[9]= new AssignInstruction(new PointerVariable("x"), new PointerVariable("t1")); // x := t1
		instr[10]= new PutNilInstruction(new PointerVariable("y"), new Selector("cdr")); // y.cdr := nil
		instr[11]= new PutSelectorInstruction(new PointerVariable("y"), new Selector("cdr"), new PointerVariable("t")); // y.cdr := t
		instr[12]= new AssignNilInstruction(new PointerVariable("t")); // t := nil
		instr[13]= new AssignNilInstruction(new PointerVariable("t1")); /// t1 := nil
		instr[14]= new DontCareInstruction(); // Just a placeholder for exit block

		// Add all the nodes
		for (FictionalIR<StaticShapeGraph> ir : instr) {
			graph.addNode(ir);
		}

		// Add all the edges
		graph.addEdge(instr[0], instr[1]);
		graph.addEdge(instr[1], instr[2]);
		graph.addEdge(instr[2], instr[3]);
		graph.addEdge(instr[3], instr[4]);
		graph.addEdge(instr[4], instr[5]);
		graph.addEdge(instr[5], instr[6]);
		graph.addEdge(instr[6], instr[7]);
		graph.addEdge(instr[7], instr[8]);
		graph.addEdge(instr[8], instr[9]);
		graph.addEdge(instr[9], instr[10]);
		graph.addEdge(instr[10], instr[11]);

		graph.addEdge(instr[12], instr[13]);
		graph.addEdge(instr[13], instr[14]);

		graph.addEdge(instr[11], instr[1]);
		graph.addEdge(instr[1], instr[12]);

		// Add initial values 
		instr[0].setInitialValue(LinkedListNormalizedCFGFactory.initialState());

		return graph;
	}

	static StaticShapeGraph initialState() {
		StaticShapeGraph initialGraph= new StaticShapeGraph();

		// This is the initial setup that we want
		// from Figure 5 of the paper
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
}
