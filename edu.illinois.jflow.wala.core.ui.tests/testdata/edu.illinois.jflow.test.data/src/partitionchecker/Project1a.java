package partitionchecker;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Simple case where we are accessing the fields of Datum object directly
 * without method calls. The only difference with Project1 is that we are using
 * String instead of int, which Wala has trouble disambiguating from int and
 * Integer sometimes.
 * 
 */
public class Project1a {

	public static void main(String[] args) {
		List<StringDatum> data = new ArrayList<StringDatum>();
		data.add(new StringDatum("1"));
		data.add(new StringDatum("2"));
		data.add(new StringDatum("3"));
		data.add(new StringDatum("4"));

		for (StringDatum d : data) {

			// Begin Stage1
			String field = d.field;
			// End Stage1

			// Begin Stage2
			String manipulatedField = produce(field);
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
