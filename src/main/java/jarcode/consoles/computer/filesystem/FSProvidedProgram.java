package jarcode.consoles.computer.filesystem;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.ProgramInstance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

// Program with normal Java functionality to it. Make sure these are safe!
// These are special kinds of files in the computer's filesystem, too.

// These need to be registered in the Computer class, so they can be restored later,
// and used on creation.

@SuppressWarnings("SpellCheckingInspection")
public abstract class FSProvidedProgram extends FSBlock {

	private static final byte ID = 0x02;

	protected static final Charset UTF_8 = Charset.forName("UTF-8");

	protected OutputStream out;
	protected InputStream in;

	private Computer computer;

	protected ProgramInstance instance;

	public FSProvidedProgram() {
		super(ID);
	}

	@Override
	public int size() {
		return 0;
	}
	public void init(OutputStream out, InputStream in, String str,
	                 Computer computer, ProgramInstance instance) throws Exception {
		this.in = in;
		this.out = out;
		this.computer = computer;
		this.instance = instance;
		run(str, computer);
	}
	public abstract void run(String str, Computer computer) throws Exception;
	protected void print(String formatted) {
		try {
			out.write(formatted.getBytes(UTF_8));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected void println(String formatted) {
		print(formatted + '\n');
	}
	protected void nextln() {
		print("\n");
	}
	protected FSBlock resolve(String input) {
		return computer.resolve(input, this);
	}
	protected boolean terminated() {
		return instance.isTerminated();
	}
	protected String handleEncapsulation(String input) {

		if (input.equals("\""))
			return input;
		else if (input.startsWith("\"") && input.endsWith("\"")) {
			return input.substring(1, input.length() - 1);
		}
		else return input;
	}
}
