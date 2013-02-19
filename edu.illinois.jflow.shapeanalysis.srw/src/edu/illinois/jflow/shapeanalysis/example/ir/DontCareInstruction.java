package edu.illinois.jflow.shapeanalysis.example.ir;

import edu.illinois.jflow.shapenalaysis.shapegraph.structures.StaticShapeGraph;

/*
 * Represents an instruction where we really do not care about its effects. This could
 * be some arithmetic instruction or some boolean instruction in an actual program that does not manipulate the heap.
 */
public class DontCareInstruction extends FictionalIR<StaticShapeGraph> {
}
