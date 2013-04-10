package partitionchecker;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Simple case where we only have one level of reference. And the loop is
 * iterating through a series of things to do. This should be parallelizable.
 * 
 */
public class Project2 {
	public static void main(String[] args) {
		List<Datum> data = new ArrayList<Datum>();
		data.add(new Datum(new Integer(1)));
		data.add(new Datum(new Integer(2)));
		data.add(new Datum(new Integer(3)));
		data.add(new Datum(new Integer(4)));

		for (Datum d : data) {

			// Begin Stage1
			Integer field = d.getField();
			// End Stage1

			// Begin Stage2
			Integer manipulatedField = produce(field);
			// End Stage2

			// Begin Stage3
			d.setField(manipulatedField);
			// End Stage3
		}
	}

	static Integer produce(Integer input) {
		return input + 2;
	}
}