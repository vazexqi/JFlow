package pdg;

import java.util.ArrayList;
import java.util.List;

public class Project7 {
	public static void main(String[] args) {
		List<Integer> list = new ArrayList<Integer>();

		int a = producer(1);
		list.add(a);

		int b = producer(a);
		list.add(b);
	}

	static int producer(int input) {
		return input + 2;
	}
}
