package partitionchecker;

class A {
	int field;

	void mutateField() {
		field += 1;
	}
}

public class Project7 {
	public static void main(String[] args) {
		for (int i = 0; i < 100; i++) {
			// Begin Stage1
			A a = new A();
			// End Stage1
			
			// Begin Stage2
			a.mutateField();
			// End Stage2
		
			// Begin Stage3
			a.mutateField();
			// End Stage3
		}
	}
}
