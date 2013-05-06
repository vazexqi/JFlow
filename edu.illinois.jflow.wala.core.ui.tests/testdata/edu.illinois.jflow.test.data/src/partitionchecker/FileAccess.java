package partitionchecker;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileAccess {
	public static void main(String[] files) throws IOException {

		for (String inputFile : files) {
			// Begin Stage1
			InputStream fileInputStream = new BufferedInputStream(
					new FileInputStream(inputFile));
			fileInputStream.close();
			// End Stage1

			// Begin Stage2
			InputStream compressedInputStream = new BufferedInputStream(
					new FileInputStream(inputFile));
			compressedInputStream.close();
			// End Stage2

		}

	}

}
