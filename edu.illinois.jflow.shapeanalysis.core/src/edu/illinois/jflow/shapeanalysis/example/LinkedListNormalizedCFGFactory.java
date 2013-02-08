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

/**
 * Returns a graph with FictionalIR as the nodes. The edges between the nodes represent control
 * flow. This is basically a hard coded vesrion of the CFG from Figure 5 of the paper
 * "Solving Shape-Analysis Problems in Languages with Destructive Updating".
 * 
 * @author nchen
 * 
 */
public class LinkedListNormalizedCFGFactory {
	static Graph<FictionalIR> createCFG() {
		Graph<FictionalIR> graph= SlowSparseNumberedGraph.make();
		FictionalIR[] i= new FictionalIR[15];

		i[0]= new AssignNilInstruction(new PointerVariable("y")); // y := nil
		i[1]= new DontCareInstruction(); // x != nil
		i[2]= new AssignNilInstruction(new PointerVariable("t")); // t := nil
		i[3]= new AssignInstruction(new PointerVariable("t"), new PointerVariable("y")); // t := y
		i[4]= new AssignNilInstruction(new PointerVariable("y")); // y := nil
		i[5]= new AssignInstruction(new PointerVariable("y"), new PointerVariable("x")); // y := x
		i[6]= new AssignNilInstruction(new PointerVariable("t1")); // t1 := nil
		i[7]= new GetSelectorInstruction(new PointerVariable("t1"), new Selector("cdr"), new PointerVariable("x")); // t1 := x.cdr
		i[8]= new AssignNilInstruction(new PointerVariable("x")); // x := nil
		i[9]= new AssignInstruction(new PointerVariable("x"), new PointerVariable("t1")); // x := t1
		i[10]= new PutNilInstruction(new PointerVariable("y"), new Selector("cdr")); // y.cdr := nil
		i[11]= new PutSelectorInstruction(new PointerVariable("y"), new Selector("cdr"), new PointerVariable("t")); // y.cdr := t
		i[12]= new AssignNilInstruction(new PointerVariable("t")); // t := nil
		i[13]= new AssignNilInstruction(new PointerVariable("t1")); /// t1 := nil
		i[14]= new DontCareInstruction(); // Just a placeholder for exit block

		// Add all the nodes
		for (FictionalIR ir : i) {
			graph.addNode(ir);
		}

		// Add all the edges
		graph.addEdge(i[0], i[1]);
		graph.addEdge(i[1], i[2]);
		graph.addEdge(i[2], i[3]);
		graph.addEdge(i[3], i[4]);
		graph.addEdge(i[4], i[5]);
		graph.addEdge(i[5], i[6]);
		graph.addEdge(i[6], i[7]);
		graph.addEdge(i[7], i[8]);
		graph.addEdge(i[8], i[9]);
		graph.addEdge(i[9], i[10]);
		graph.addEdge(i[10], i[11]);

		graph.addEdge(i[12], i[13]);
		graph.addEdge(i[13], i[14]);

		graph.addEdge(i[11], i[1]);
		graph.addEdge(i[1], i[12]);

		return graph;
	}
}
