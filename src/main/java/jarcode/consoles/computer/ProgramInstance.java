package jarcode.consoles.computer;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.interpreter.InterpretedProgram;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

// immutable, except for the termination state.
public class ProgramInstance implements Runnable {

	public final InputStream stdin;
	public final OutputStream stdout;

	public final InputStream in;
	public final OutputStream out;

	final FSProvidedProgram provided;
	final InterpretedProgram interpreted;

	private final Thread thread = new Thread(this);

	private final String argument;

	private final Computer computer;

	private String data = null;

	private volatile boolean terminated = false;

	{
		thread.setDaemon(true);
		thread.setName("Program Thread");
		thread.setPriority(Thread.MIN_PRIORITY);
	}

	public ProgramInstance(FSProvidedProgram provided, String argument, Computer computer) {
		stdin = new LinkedStream();
		out = ((LinkedStream) stdin).createOutput();
		in = new LinkedStream();
		stdout = ((LinkedStream) in).createOutput();
		this.provided = provided;
		interpreted = null;
		this.argument = argument;
		this.computer = computer;
	}
	public ProgramInstance(InterpretedProgram interpreted, String argument, Computer computer) {
		stdin = new LinkedStream();
		out = ((LinkedStream) stdin).createOutput();
		in = new LinkedStream();
		stdout = ((LinkedStream) in).createOutput();
		provided = null;
		this.interpreted = interpreted;
		this.argument = argument;
		this.computer = computer;
	}
	public ProgramInstance(InterpretedProgram interpreted, String argument, Computer computer, String data) {
		this(interpreted, argument, computer);
		this.data = data;
	}
	public void start() {
		thread.start();
	}
	public void terminate() {
		terminated = true;
	}
	public boolean isTerminated() {
		return terminated;
	}
	public void waitFor() throws InterruptedException{
		thread.join();
	}
	@Override
	public void run() {
		try {
			if (provided != null)
				provided.init(stdout, stdin, argument, computer, this);
			else if (interpreted != null) {
				if (data == null)
					interpreted.run(stdout, stdin, argument, computer, this);
				else
					interpreted.runRaw(stdout, stdin, argument, computer, this, data);
			}
			Terminal terminal = computer.getTerminal(this);
			terminal.waitFor();
		}
		catch (Throwable e) {
			write(e.getClass().getSimpleName() + (e.getCause() == null ? "" :  ", caused by " + e.getCause()));
			Consoles.getInstance().getLogger().severe("Error while executing program:");
			if (Consoles.debug)
				e.printStackTrace();
		}
		finally {
			try {
				stdout.write((byte) -1); // write -1 (EOF) to signal stream end
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	private void write(String text) {
		try {
			stdout.write(text.getBytes(Charset.forName("UTF-8")));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean contains(Object another) {
		return another instanceof FSProvidedProgram ?
				provided == another : another instanceof InterpretedProgram && interpreted == another;
	}
}
