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
		data.add(new Datum(1));
		data.add(new Datum(2));
		data.add(new Datum(3));
		data.add(new Datum(4));

		for (Datum d : data) {

			// Begin Stage1
			int field = d.getField();
			// End Stage1

			// Begin Stage2
			int manipulatedField = produce(field);
			// End Stage2

			// Begin Stage3
			d.setField(manipulatedField);
			System.out.println(d);
			// End Stage3
		}
	}

	static int produce(int input) {
		return input + 2;
	}
}