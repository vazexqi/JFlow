public class Item<T> {
	T value;

	Item(T value) {
		this.value = value;
	}

	T getValue() {
		return value;
	}

	void setValue(T value) {
		this.value = value;
	}
}
