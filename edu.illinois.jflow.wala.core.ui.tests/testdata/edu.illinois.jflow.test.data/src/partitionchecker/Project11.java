package partitionchecker;

import java.util.ArrayList;
import java.util.List;

public class Project11 {
	T field = new T();

	public static void main(String[] args) {
		Project11 p = new Project11();
		p.method();
	}

	void method() {
		List<StringDatum> data = new ArrayList<StringDatum>();
		for (StringDatum d : data) {
			// Begin Stage1
			T tempField = field;
			tempField.field = 2;
			// End Stage1

			// Begin Stage2
			tempField.field = 4;
			// End Stage2
		}
	}
}

class T {
	int field;
}
