package partitionchecker;

public class RecipePartialTransfer {
	List instructions, photos;

	static void main(String[] args) {
		for (int i = 0; i < 100; i++) {
			// Begin Node1
			RecipePartialTransfer r = new RecipePartialTransfer();
			List tempInstruction = r.instructions;
			// End Node1

			// Begin Node2
			tempInstruction.elements[0] = new Object(); // modification
			// End Node2

			// Begin Node3
			Object[] local = tempInstruction.elements;
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

	public RecipePartialTransfer() {
		// TODO Auto-generated constructor stub
		instructions = new List();
		photos = new List();

		for (int i = 0; i < 100; i++) {
			Instruction instr = new Instruction();
			instructions.elements[i] = instr;
		}
	}
}