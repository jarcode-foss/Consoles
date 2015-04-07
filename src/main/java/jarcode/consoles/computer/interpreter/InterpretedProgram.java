package jarcode.consoles.computer.interpreter;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class InterpretedProgram {

	private FSFile file;

	public InterpretedProgram(FSFile file) {
		this.file = file;
	}
	public void run(OutputStream out, InputStream in, String str, Computer computer) throws Exception {
		//TODO: finish
		Thread.sleep(1000);
		out.write("Done.".getBytes(Charset.forName("UTF-8")));
	}
}
