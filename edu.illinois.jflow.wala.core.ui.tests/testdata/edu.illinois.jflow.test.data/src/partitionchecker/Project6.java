package partitionchecker;

public class Project6 {
	private static Project6 SHARED = new Project6("SHARED");

	private String field;

	public Project6(String fieldArg) {
		this.field = fieldArg;
	}

	public void mutateField() {
		field = field + "mutated";
	}

	public static void main(String[] args) {
		// Deliberately iterate through args so that we cannot determine in
		// advanced what we point to
		for (String s : args) {

			// Begin Stage1
			Project6 p = new Project6(s);
			SHARED.mutateField();
			// End Stage1

			// Begin Stage2
			p.mutateField();
			SHARED.mutateField();
			// End Stage2
		}
	}
}
