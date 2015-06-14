package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.manual.ProvidedManual;

@ProvidedManual(
		author = "Jarcode",
		version = "1.1",
		contents = "Clears the content in the current terminal session"
)
public class ClearProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		Terminal terminal = computer.getCurrentTerminal();
		if (terminal != null)
			terminal.clear();
	}
}
