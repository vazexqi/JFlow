import java.util.ArrayList;
import java.util.List;

public class PipelineCase3 {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	void pipeline() {
		System.out.println("Prologue");

		for (Item<Integer> item : items) {
			int a = item.getValue();

			int b = method1(a);

			int c = method2(a);

			int d = method3(b, c);

		}

		System.out.println("Epilogue");
	}

	private int method3(int value1, int value2) {
		return value1 + value2;
	}

	private int method2(int value) {
		return value + 2;
	}

	private Integer method1(int value) {
		return value + 1;
	}
}
