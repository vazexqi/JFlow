package partitionchecker;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Simple case where we only have one level of reference. And the loop is
 * iterating through a series of things to do. This should NOT be parallelizable
 * because of the loop-carried dependency.
 * 
 */
public class Project2LoopCarriedDependency {
	public static void main(String[] args) {
		List<Datum> data = new ArrayList<Datum>();
		data.add(new Datum(1));
		data.add(new Datum(2));
		data.add(new Datum(3));
		data.add(new Datum(4));

		Integer loopCarriedDependency = 0;
		for (Datum d : data) {

			// Begin Stage1
			Integer field = d.getField();
			// End Stage1

			// Begin Stage2
			Integer manipulatedField = produce(field);
			// End Stage2

			// Begin Stage3
			d.setField(manipulatedField);
			loopCarriedDependency++;
			// End Stage3
		}
	}

	static Integer produce(Integer input) {
		return input + 2;
	}
}