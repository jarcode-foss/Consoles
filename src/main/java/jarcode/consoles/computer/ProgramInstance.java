package jarcode.consoles.computer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

// immutable
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
	public void start() {
		thread.start();
	}
	public void waitFor() throws InterruptedException{
		thread.join();
	}
	@Override
	public void run() {
		try {
			if (provided != null)
				provided.init(stdout, stdin, argument, computer);
			else if (interpreted != null)
				interpreted.run(stdout, stdin, argument, computer);
		}
		catch (Throwable e) {
			write(e.getClass().getSimpleName() + (e.getCause() == null ? "" :  ", caused by " + e.getCause()));
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
