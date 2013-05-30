package partitionchecker;

import java.util.ArrayList;
import java.util.List;

public class Project8 {
	int field;

	public static void main(String[] args) {
		List<StringDatum> data = new ArrayList<StringDatum>();
		data.add(new StringDatum("1"));
		data.add(new StringDatum("2"));
		data.add(new StringDatum("3"));
		data.add(new StringDatum("4"));
		Project8 p = new Project8();

		for (StringDatum d : data) {
			// Begin Stage1
			String tempField = d.field;
			p.field = 2;
			// End Stage1

			// Begin Stage2
			String manipulatedField = produce(tempField);
			p.field = 4;
			// End Stage2

			// Begin Stage3
			d.field = manipulatedField;
			// End Stage3
		}
	}

	static String produce(String input) {
		return "[" + input + "]";
	}
}
