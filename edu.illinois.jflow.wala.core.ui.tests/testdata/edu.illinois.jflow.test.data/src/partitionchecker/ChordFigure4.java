package partitionchecker;

/*
 * This example is from Figure 4 of the paper "Effective Race Detection for Java".
 * I want to verify if our mod/ref does a close enough approximation of what their algorithm does.
 */
public class ChordFigure4 {
	Integer f;

	ChordFigure4() {
		this.f = 0;
	}

	Integer rd() {
		return this.f;
	}

	Integer wr(Integer x) {
		this.f = x;
		return x;
	}

	Integer get() {
		return this.rd();
	}

	Integer inc() {
		// We change it so that we don't add because Wala's type inference has problems with adding
		Integer t = this.rd();
		(new ChordFigure4()).wr(1); // just do this for no reason
		return this.wr(t);
	}
	
	public static void main(String[] args) {
		ChordFigure4 cf;
		cf = new ChordFigure4();
		cf.get();
		cf.inc();
	}
}
