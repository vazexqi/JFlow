package partitionchecker;

public class Project5 {

	private String field;

	public Project5(String fieldArg) {
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
			Project5 p = new Project5(s);
			// End Stage1

			// Begin Stage2
			p.mutateField();
			// End Stage2
		}
	}
}
