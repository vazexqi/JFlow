package pdg;

public class Project4 {
	public static void main(String[] args) {
		int a = producer(1);
		int b = producer(a);
		a += 2;
		int c = producer(a);
	}

	static int producer(int input) {
		return input + 2;
	}
}
