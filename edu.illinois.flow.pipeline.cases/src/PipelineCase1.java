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

			method3(a, b, c);

		}

		System.out.println("Epilogue");
	}

	private void method3(int value1, int value2, int value3) {
		int i = value1 + value2 + value3;
		System.out.println(i);
	}

	private int method2(int value) {
		return value + 2;
	}

	private int method1(int value) {
		return value + 1;
	}

	public static void main(String[] args) throws InterruptedException {
		PipelineCase1 pipe = new PipelineCase1();

		for (int i = 0; i < 100; i++)
			pipe.items.add(new Item<Integer>(i));

		pipe.pipeline();
	}
}
