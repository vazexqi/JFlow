package partitionchecker;

class StringDatum {
	String field;

	public StringDatum(String field) {
		this.field = field;
	}

	@Override
	public String toString() {
		return "Datum [field=" + field + "]";
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

}