package basic_out;

import groovyx.gpars.DataflowMessagingRunnable;

public class EC_testProject1 {
	public static void main(String[] args) {
		int a = producer(1);
		new DataflowMessagingRunnable(1) {
			protected void doRun(Object... arguments) {
				/*[*/
				int b = producer(((Integer) arguments[0]));
				int c = producer(b);
				/*]*/
			}
		}.call(a);
	}

	static int producer(int input) {
		return input + 2;
	}
}
