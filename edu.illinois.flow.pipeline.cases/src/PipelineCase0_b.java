import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.Dataflow;
import groovyx.gpars.dataflow.DataflowQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PipelineCase0_b {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	@SuppressWarnings("serial")
	void pipeline() throws InterruptedException {
		System.out.println("Prologue");

		final CountDownLatch latch = new CountDownLatch(1);

		final DataflowQueue<Item<Integer>> channel0 = new DataflowQueue<Item<Integer>>();
		final DataflowQueue<Integer> channel1 = new DataflowQueue<Integer>();
		final DataflowQueue<Integer> channel2 = new DataflowQueue<Integer>();
		final DataflowQueue<Integer> channel3 = new DataflowQueue<Integer>();

		Dataflow.operator(channel0, channel1, new DataflowMessagingRunnable(1) {

			@SuppressWarnings("unchecked")
			@Override
			protected void doRun(Object[] arguments) {
				channel1.bind(method0((Item<Integer>) arguments[0]));
			}
		});

		Dataflow.operator(channel1, channel2, new DataflowMessagingRunnable(1) {

			@Override
			protected void doRun(Object[] arguments) {
				channel2.bind(method1((Integer) arguments[0]));
			}
		});

		Dataflow.operator(channel2, channel3, new DataflowMessagingRunnable(1) {

			@Override
			protected void doRun(Object[] arguments) {
				channel3.bind(method2((Integer) arguments[0]));
			}
		});

		Dataflow.operator(channel3, null, new DataflowMessagingRunnable(1) {

			AtomicInteger counter = new AtomicInteger(100);

			@Override
			protected void doRun(Object[] arguments) {
				method3((Integer) arguments[0]);
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
