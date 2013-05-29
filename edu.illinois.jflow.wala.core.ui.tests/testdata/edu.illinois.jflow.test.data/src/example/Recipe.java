package example;

class Recipe {
	List instructions, photos;

	static void main(String[] args) {
		for (int i = 0; i < 100; i++) {
			// Begin Node1
			Recipe r = new Recipe();
			// End Node1

			// Begin Node2
			r.updateInstructions(); // Modifies instructions
			r.updatePhotos(); // Modified photos
			// End Node2

			// Begin Node3
			r.display(); // Reads instructions and photos
			// End Node3
		}
	}

	private void display() {
		// Read photos and instructions
		Object[] photoTemp = photos.elements;
		Object[] instructionTemp = instructions.elements;
	}

	private void updatePhotos() {
		// Modifies photos, trivially
		photos.elements[0] = new Object();
	}

	private void updateInstructions() {
		// Modified instructions, trivially
		instructions.elements[0] = new Object();
	}

	Recipe() {
		instructions = new List();
		photos = new List();

		for (int i = 0; i < 100; i++) {
			Instruction instr = new Instruction();
			instructions.elements[i] = instr;
		}
	}
}

class List {
	Object[] elements;

	List() {
		Object[] temp = new Object[10];
		elements = temp;
	}
}

class Instruction {

}