import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.DataflowQueue;

import java.util.ArrayList;
import java.util.List;

public class PipelineCase3 {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	@SuppressWarnings("serial")
	void pipeline() throws InterruptedException {
		System.out.println("Prologue");

		for (Item<Integer> item : items) {
			final DataflowQueue<Integer> channel1 = new DataflowQueue<Integer>();
			new DataflowMessagingRunnable(1) {

				@SuppressWarnings("unchecked")
				@Override
				protected void doRun(Object[] arguments) {
					channel1.bind(method0((Item<Integer>) arguments[0]));
				}
			}.call(item);
			int a = channel1.getVal();

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
