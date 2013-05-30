package partitionchecker;

import java.util.ArrayList;
import java.util.List;

public class Project9 {
	int field;

	public static void main(String[] args) {
		Project9 p = new Project9();
		p.method();
	}

	static String produce(String input) {
		return "[" + input + "]";
	}
	
	void method() {
		List<StringDatum> data = new ArrayList<StringDatum>();
		for (StringDatum d : data) {
			// Begin Stage1
			String tempField = d.field;
			field = 2;
			// End Stage1

			// Begin Stage2
			String manipulatedField = produce(tempField);
			field = 4;
			// End Stage2

			// Begin Stage3
			d.field = manipulatedField;
			// End Stage3
		}
	}
}
