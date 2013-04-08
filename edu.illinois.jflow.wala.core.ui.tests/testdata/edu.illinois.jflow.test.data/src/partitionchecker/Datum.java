package partitionchecker;

class Datum {
	int field;

	public Datum(int field) {
		this.field = field;
	}

	@Override
	public String toString() {
		return "Datum [field=" + field + "]";
	}

	public int getField() {
		return field;
	}

	public void setField(int field) {
		this.field = field;
	}

}