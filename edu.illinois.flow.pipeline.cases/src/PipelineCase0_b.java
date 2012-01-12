import java.util.ArrayList;
import java.util.List;

public class PipelineCase0_b {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	void pipeline() {
		System.out.println("Prologue");

		for (Item<Integer> item : items) {
			int a = method0(item);

			int b = method1(a);

			int c = method2(b);

			method3(c);

		}

		System.out.println("Epilogue");
	}

	private int method0(Item<Integer> item) {
		int temp1 = (Integer) item.getValue();
		int temp2 = 2;
		int a = temp1 + temp2;
		return a;
	}

	private void method3(int value) {
		System.out.println(value);
	}

	private Integer method2(int value) {
		return value + 2;
	}

	private Integer method1(int value) {
		return value + 1;
	}

	public static void main(String[] args) {
		PipelineCase0_b pipe = new PipelineCase0_b();

		for (int i = 0; i < 100; i++)
			pipe.items.add(new Item<Integer>(i));

		pipe.pipeline();
	}
}
