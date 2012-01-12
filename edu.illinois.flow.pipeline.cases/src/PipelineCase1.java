import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.Dataflow;
import groovyx.gpars.dataflow.DataflowQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PipelineCase1 {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	@SuppressWarnings("serial")
	void pipeline() throws InterruptedException {
		System.out.println("Prologue");

		final CountDownLatch latch = new CountDownLatch(1);

		final DataflowQueue<Token> channel0 = new DataflowQueue<Token>();
		final DataflowQueue<Token> channel1 = new DataflowQueue<Token>();
		final DataflowQueue<Token> channel2 = new DataflowQueue<Token>();
		final DataflowQueue<Token> channel3 = new DataflowQueue<Token>();

		Dataflow.operator(channel0, channel1, new DataflowMessagingRunnable(1) {

			@Override
			protected void doRun(Object[] arguments) {
				Token token = (Token) arguments[0];
				int method0 = method0(token.getItem());
				token.setA(method0);
				channel1.bind(token);
			}
		});

		Dataflow.operator(channel1, channel2, new DataflowMessagingRunnable(1) {

			@Override
			protected void doRun(Object[] arguments) {
				Token token = (Token) arguments[0];
				int method1 = method1(token.getA());
				token.setB(method1);
				channel2.bind(token);
			}
		});

		Dataflow.operator(channel2, channel3, new DataflowMessagingRunnable(1) {

			@Override
			protected void doRun(Object[] arguments) {
				Token token = (Token) arguments[0];
				int method2 = method2(token.getB());
				token.setC(method2);
				channel3.bind(token);
			}
		});

		Dataflow.operator(channel3, null, new DataflowMessagingRunnable(1) {
			AtomicInteger counter = new AtomicInteger(100);

			@Override
			protected void doRun(Object[] arguments) {
				Token token = (Token) arguments[0];
				method3(token.getA(), token.getB(), token.getC());
				counter.getAndDecrement();
				if (counter.get() == 0)
					latch.countDown();
			}
		});

		for (Item<Integer> item : items) {
			Token token = new Token();
			token.setItem(item);
			channel0.bind(token);

		}

		latch.await();

		System.out.println("Epilogue");
	}

	class Token {
		Item<Integer> item;
		Integer a;
		Integer b;
		Integer c;

		public Item<Integer> getItem() {
			return item;
		}

		public void setItem(Item<Integer> item) {
			this.item = item;
		}

		public Integer getA() {
			return a;
		}

		public void setA(Integer a) {
			this.a = a;
		}

		public Integer getB() {
			return b;
		}

		public void setB(Integer b) {
			this.b = b;
		}

		public Integer getC() {
			return c;
		}

		public void setC(Integer c) {
			this.c = c;
		}

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
		PipelineCase1 pipe = new PipelineCase1();

		for (int i = 0; i < 100; i++)
			pipe.items.add(new Item<Integer>(i));

		pipe.pipeline();
	}
}
