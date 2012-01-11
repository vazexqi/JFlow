import java.util.ArrayList;
import java.util.List;

public class PipelineCase2 {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	void pipeline() {
		System.out.println("Prologue");

		for (Item<Integer> item : items) {
			int a;
			if (somecondition(item)) {
				a = 1;
			} else {
				a = 2;
			}

			int b = method1(a);

			int c = method2(b);

		}

		System.out.println("Epilogue");
	}

	private boolean somecondition(Item<Integer> item) {
		return item.getValue() > 5;
	}

	private int method2(int value) {
		return value + 2;
	}

	private Integer method1(int value) {
		return value + 1;
	}
}
