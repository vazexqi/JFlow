package pdg;

public class Project5 {
	public static void entry(int param) {
		int a = producer(param);
		int b = producer(a);
		int c = producer(b);
	}

	static int producer(int input) {
		return input + 2;
	}
}
