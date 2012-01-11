import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.DataflowQueue;

import java.util.ArrayList;
import java.util.List;

public class PipelineCase0 {
	List<Item<Integer>> items = new ArrayList<Item<Integer>>();

	@SuppressWarnings("serial")
	void pipeline() throws InterruptedException {
		System.out.println("Prologue");

		for (Item<Integer> item : items) {

			Token token = new Token();
			token.setItem(item);

			final DataflowQueue<Token> channel1 = new DataflowQueue<Token>();
			new DataflowMessagingRunnable(1) {

				@Override
				protected void doRun(Object[] arguments) {
					Token token = (Token) arguments[0];
					int method0 = method0(token.getItem());
					token.setA(method0);
					channel1.bind(token);
				}
			}.call(token);

			final DataflowQueue<Token> channel2 = new DataflowQueue<Token>();
			new DataflowMessagingRunnable(1) {

				@Override
				protected void doRun(Object[] arguments) {
					Token token = (Token) arguments[0];
					Integer method1 = method1(token.getA());
					token.setB(method1);
					channel2.bind(token);
				}
			}.call(channel1.getVal());

			final DataflowQueue<Token> channel3 = new DataflowQueue<Token>();
			new DataflowMessagingRunnable(1) {

				@Override
				protected void doRun(Object[] arguments) {
					Token token = (Token) arguments[0];
					Integer method2 = method2(token.getB());
					token.setC(method2);
					channel3.bind(token);
				}
			}.call(channel2.getVal());

			new DataflowMessagingRunnable(1) {

				@Override
				protected void doRun(Object[] arguments) {
					Token token = (Token) arguments[0];
					method3(token.getC());

				}
			}.call(channel3.getVal());

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
		PipelineCase0 pipe = new PipelineCase0();

		for (int i = 0; i < 100; i++)
			pipe.items.add(new Item<Integer>(i));

		pipe.pipeline();
	}
}
