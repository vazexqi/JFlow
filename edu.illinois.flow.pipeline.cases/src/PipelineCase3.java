import java.util.ArrayList;
import java.util.List;

public class PipelineCase3 {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	void pipeline() {
		System.out.println("Prologue");

		for (Item<Integer> item : items) {
			int a = method0(item);

			int b = method1(item);

			int c = method2(item);

			method3(a, b, c);

		}

		System.out.println("Epilogue");
	}

	private void method3(int value1, int value2, int value3) {
		System.out.println(value1 + value2 + value3);
	}

	private int method2(Item<Integer> item) {
		return item.getValue() + 2;
	}

	private Integer method1(Item<Integer> item) {
		return item.getValue() + 1;
	}

	private int method0(Item<Integer> item) {
		return item.getValue() + 1;
	}

	public static void main(String[] args) throws InterruptedException {
		PipelineCase3 pipe = new PipelineCase3();

		for (int i = 0; i < 100; i++)
			pipe.items.add(new Item<Integer>(i));

		pipe.pipeline();
	}
}
