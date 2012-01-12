import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.DataflowQueue;

import java.util.ArrayList;
import java.util.List;

public class PipelineCase1_b {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	@SuppressWarnings("serial")
	void pipeline() throws InterruptedException {
		System.out.println("Prologue");

		for (Item<Integer> item : items) {
			final DataflowQueue<Integer> channel1_a = new DataflowQueue<Integer>();
			final DataflowQueue<Integer> channel1_b = new DataflowQueue<Integer>();
			new DataflowMessagingRunnable(1) {

				@SuppressWarnings("unchecked")
				@Override
				protected void doRun(Object[] arguments) {
					int method0 = method0((Item<Integer>) arguments[0]);
					channel1_a.bind(method0);
					channel1_b.bind(method0);
				}
			}.call(item);

			final DataflowQueue<Integer> channel2_a = new DataflowQueue<Integer>();
			final DataflowQueue<Integer> channel2_b = new DataflowQueue<Integer>();
			new DataflowMessagingRunnable(1) {

				@Override
				protected void doRun(Object[] arguments) {
					int method1 = method1((Integer) arguments[0]);
					channel2_a.bind(method1);
					channel2_b.bind(method1);
				}
			}.call(channel1_a.getVal());

			int c = method2(channel2_a.getVal());

			method3(channel1_b.getVal(), channel2_b.getVal(), c);

		}

		System.out.println("Epilogue");
	}

	private int method0(Item<Integer> item) {
		int temp1 = (Integer) item.getValue();
		int temp2 = 2;
		int a = temp1 + temp2;
		return a;
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
		PipelineCase1_b pipe = new PipelineCase1_b();

		for (int i = 0; i < 100; i++)
			pipe.items.add(new Item<Integer>(i));

		pipe.pipeline();
	}
}
