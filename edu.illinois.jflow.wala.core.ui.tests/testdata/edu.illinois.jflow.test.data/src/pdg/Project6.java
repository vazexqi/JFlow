package pdg;

import java.util.List;

public class Project6 {
	public static void entry(List<Integer> param) {
		int a = producer(param.get(0));
		int b = producer(a);
		int c = producer(b);
	}

	static int producer(int input) {
		return input + 2;
	}

	public static void main(String[] args) {
		// Does nothing, needed to initiate call graph
		entry(null);
	}
}
