package partitionchecker;

/**
 * 
 * Simple case where we only have one level of reference. And the loop is
 * iterating through a series of things to do from an array. This should be
 * parallelizable.
 * 
 * Question is: can/should we handle it? array references are harder to handle
 * 
 */
public class Project3 {
	public static void main(String[] args) {
		Datum[] data = new Datum[] { new Datum(1), new Datum(2), new Datum(3),
				new Datum(4) };
		for (int i = 0; i < data.length; i++) {

			// Begin Stage1
			int field = data[i].getField();
			// End Stage1

			// Begin Stage2
			int manipulatedField = produce(field);
			// End Stage2

			// Begin Stage3
			data[i].setField(manipulatedField);
			System.out.println(data[i]);
			// End Stage3
		}
	}

	static int produce(int input) {
		return input + 2;
	}
}