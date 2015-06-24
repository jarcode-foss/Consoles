package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.filesystem.FSProvidedProgram;
import ca.jarcode.consoles.computer.manual.ProvidedManual;

@ProvidedManual(
		author = "Jarcode",
		version = "1.1",
		contents = "Prints a random joke"
)
@Deprecated
public class JokeProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		Terminal terminal = computer.getCurrentTerminal();
		terminal.printRandomJoke();
	}
}
