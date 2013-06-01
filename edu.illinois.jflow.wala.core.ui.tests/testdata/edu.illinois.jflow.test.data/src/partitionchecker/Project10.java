package partitionchecker;

import java.util.ArrayList;
import java.util.List;

public class Project10 {
	D field = new D();

	public static void main(String[] args) {
		Project10 p = new Project10();
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
			field.setField(2);
			// End Stage1

			// Begin Stage2
			String manipulatedField = produce(tempField);
			field.setField(3);
			// End Stage2

			// Begin Stage3
			d.field = manipulatedField;
			// End Stage3
		}
	}
}

class D {
	Integer field;

	void setField(Integer x) {
		field = x;
	}
}