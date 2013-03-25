package analyzer;

public class Project6 {
	public static void main(String[] args) {
		int a;
		int b;
		int c;

		a = producer(1);
		b = producer(a);
		a += b;
		c = consumer(a, b);
	}

	static int producer(int input) {
		return input + 2;
	}

	static int consumer(int input1, int input2) {
		return input1 + input2;
	}
}