package partitionchecker;

public class Project4 {
	public static void main(String[] args) {
		entry(new Datum[] { new Datum(new Integer(1)),
				new Datum(new Integer(2)), new Datum(new Integer(3)),
				new Datum(new Integer(4)) });
	}

	static void entry(Datum[] data) {
		for (Datum d : data) {

			// Begin Stage1
			Integer field = d.field;
			// End Stage1

			// Begin Stage2
			Integer manipulatedField = produce(d);
			// End Stage2

			// Begin Stage3
			d.field = manipulatedField;
			// End Stage3
		}
	}

	static Integer produce(Datum input) {
		return input.field;

	}
}
