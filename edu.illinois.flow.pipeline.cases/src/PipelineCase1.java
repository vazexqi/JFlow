import java.util.ArrayList;
import java.util.List;

public class PipelineCase1 {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	void pipeline() {
		System.out.println("Prologue");

		for (Item<Integer> item : items) {
			int temp1 = (Integer) item.getValue();
			int temp2 = 2;
			int a = temp1 + temp2;

			int b = method1(a);

			int c = method2(b);

			int d = method3(a, b, c);

		}

		System.out.println("Epilogue");
	}

	private int method3(int value1, int value2, int value3) {
		return value1 + value2 + value3;
	}

	private int method2(int value) {
		return value + 2;
	}

	private Integer method1(int value) {
		return value + 1;
	}
}
