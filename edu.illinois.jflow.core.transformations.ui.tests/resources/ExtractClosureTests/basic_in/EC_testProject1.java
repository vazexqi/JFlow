package basic_in;

public class EC_testProject1 {
	public static void main(String[] args) {
		int a = producer(1);
		/*[*/
		int b = producer(a);
		int c = producer(b);
		/*]*/
	}

	static int producer(int input) {
		return input + 2;
	}
}
