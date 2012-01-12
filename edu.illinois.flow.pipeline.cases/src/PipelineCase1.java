import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.DataflowQueue;

import java.util.ArrayList;
import java.util.List;

public class PipelineCase1 {
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

			new DataflowMessagingRunnable(3) {

				@Override
				protected void doRun(Object[] arguments) {
					method3((Integer) arguments[0], (Integer) arguments[1],
							(Integer) arguments[2]);
				}
			}.call(new Object[] { a, b, c });

		}

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
