import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.Dataflow;
import groovyx.gpars.dataflow.DataflowQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PipelineCase3 {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	@SuppressWarnings({ "serial", "unchecked" })
	void pipeline() throws InterruptedException {
		System.out.println("Prologue");

		final CountDownLatch latch = new CountDownLatch(1);

		// These are the channels for communicating the item to each one
		final DataflowQueue<Item<Integer>> channel0_a = new DataflowQueue<Item<Integer>>();
		final DataflowQueue<Item<Integer>> channel0_b = new DataflowQueue<Item<Integer>>();
		final DataflowQueue<Item<Integer>> channel0_c = new DataflowQueue<Item<Integer>>();

		final DataflowQueue<Integer> channel1 = new DataflowQueue<Integer>();
		final DataflowQueue<Integer> channel2 = new DataflowQueue<Integer>();
		final DataflowQueue<Integer> channel3 = new DataflowQueue<Integer>();

		Dataflow.operator(channel0_a, channel1,
				new DataflowMessagingRunnable(1) {

					@Override
					protected void doRun(Object[] arguments) {
						channel1.bind(method0((Item<Integer>) arguments[0]));
					}
				});

		Dataflow.operator(channel0_b, channel2,
				new DataflowMessagingRunnable(1) {

					@Override
					protected void doRun(Object[] arguments) {
						channel2.bind(method1((Item<Integer>) arguments[0]));
					}

				});

		Dataflow.operator(channel0_c, channel3,
				new DataflowMessagingRunnable(1) {

					@Override
					protected void doRun(Object[] arguments) {
						channel3.bind(method2((Item<Integer>) arguments[0]));
					}
				});

		Dataflow.operator(Arrays.asList(channel1, channel2, channel3), null,
				new DataflowMessagingRunnable(3) {

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
			channel0_a.bind(item);
			channel0_b.bind(item);
			channel0_c.bind(item);
		}

		latch.await();

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
