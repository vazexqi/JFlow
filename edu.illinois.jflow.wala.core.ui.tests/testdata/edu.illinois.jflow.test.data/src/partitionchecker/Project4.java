package partitionchecker;

public class Project4 {
	public static void main(String[] args) {
		Datum[] data = new Datum[] { new Datum(1) };
		for (Datum d : data) {

			// Begin Stage1
			Integer field = d.field;
			// End Stage1

			// Begin Stage2
			Integer manipulatedField = produce(field);
			// End Stage2

			// Begin Stage3
			d.field = manipulatedField;
			// End Stage3
		}
	}

	static Integer produce(Integer input) {
		return input + 2;

	}
}
