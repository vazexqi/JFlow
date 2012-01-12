import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.DataflowQueue;

import java.util.ArrayList;
import java.util.List;

public class PipelineCase0_b {
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

			final DataflowQueue<Integer> channel2 = new DataflowQueue<Integer>();
			new DataflowMessagingRunnable(1) {

				@Override
				protected void doRun(Object[] arguments) {
					channel2.bind(method1((Integer) arguments[0]));
				}
			}.call(a);
			int b = channel2.getVal();

			final DataflowQueue<Integer> channel3 = new DataflowQueue<Integer>();
			new DataflowMessagingRunnable(1) {

				@Override
				protected void doRun(Object[] arguments) {
					channel3.bind(method2((Integer) arguments[0]));
				}
			}.call(b);
			int c = channel3.getVal();

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

	public static void main(String[] args) throws InterruptedException {
		PipelineCase0_b pipe = new PipelineCase0_b();

		for (int i = 0; i < 100; i++)
			pipe.items.add(new Item<Integer>(i));

		pipe.pipeline();
	}
}
