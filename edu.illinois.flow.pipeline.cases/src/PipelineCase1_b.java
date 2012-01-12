import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.Dataflow;
import groovyx.gpars.dataflow.DataflowQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PipelineCase1_b {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	@SuppressWarnings({ "serial", "unchecked" })
	void pipeline() throws InterruptedException {
		System.out.println("Prologue");

		final CountDownLatch latch = new CountDownLatch(1);

		final DataflowQueue<Item<Integer>> channel0 = new DataflowQueue<Item<Integer>>();
		final DataflowQueue<Integer> channel1_a = new DataflowQueue<Integer>();
		final DataflowQueue<Integer> channel1_b = new DataflowQueue<Integer>();
		final DataflowQueue<Integer> channel2_a = new DataflowQueue<Integer>();
		final DataflowQueue<Integer> channel2_b = new DataflowQueue<Integer>();
		final DataflowQueue<Integer> channel3 = new DataflowQueue<Integer>();

		Dataflow.operator(Arrays.asList(channel0),
				Arrays.asList(channel1_a, channel1_b),
				new DataflowMessagingRunnable(1) {

					@Override
					protected void doRun(Object[] arguments) {
						int method0 = method0((Item<Integer>) arguments[0]);
						channel1_a.bind(method0);
						channel1_b.bind(method0);
					}
				});

		Dataflow.operator(Arrays.asList(channel1_a),
				Arrays.asList(channel2_a, channel2_b),
				new DataflowMessagingRunnable(1) {

					@Override
					protected void doRun(Object[] arguments) {
						int method1 = method1((Integer) arguments[0]);
						channel2_a.bind(method1);
						channel2_b.bind(method1);
					}
				});

		Dataflow.operator(channel2_a, channel3,
				new DataflowMessagingRunnable(1) {

					@Override
					protected void doRun(Object[] arguments) {
						int method2 = method2((Integer) arguments[0]);
						channel3.bind(method2);
					}
				});

		Dataflow.operator(Arrays.asList(channel1_b, channel2_b, channel3),
				null, new DataflowMessagingRunnable(3) {

					AtomicInteger counter = new AtomicInteger(100);

					@Override
					protected void doRun(Object[] arguments) {
						method3((Integer) arguments[0], (Integer) arguments[1],
								(Integer) arguments[2]);
						counter.getAndDecrement();
						if (counter.get() == 0)
							latch.countDown();
					}
				});
		for (Item<Integer> item : items) {
			channel0.bind(item);
		}

		latch.await();

		System.out.println("Epilogue");
	}

	private int method0(Item<Integer> item) {
		int temp1 = item.getValue();
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
