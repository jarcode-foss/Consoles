package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

public class ClearProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		Terminal terminal = computer.getCurrentTerminal();
		if (terminal != null)
			terminal.clear();
	}
}
