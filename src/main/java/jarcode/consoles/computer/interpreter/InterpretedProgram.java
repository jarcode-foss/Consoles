package jarcode.consoles.computer.interpreter;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Program;
import jarcode.consoles.computer.filesystem.FSFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class InterpretedProgram implements Program {

	private FSFile file;

	public InterpretedProgram(FSFile file) {
		this.file = file;
	}
	public void run(OutputStream out, InputStream in, String str, Computer computer) throws Exception {
		//TODO: implement Lua interpreter
		out.write("Lua programs are not currently supported\n".getBytes(Charset.forName("UTF-8")));
		Thread.sleep(500);
		out.write("Exiting.".getBytes(Charset.forName("UTF-8")));
	}
}
