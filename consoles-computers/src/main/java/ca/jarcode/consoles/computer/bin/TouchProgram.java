package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.ProgramUtils;
import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.filesystem.*;
import ca.jarcode.consoles.computer.manual.ProvidedManual;

import java.util.Arrays;
import java.util.stream.Collectors;

import static ca.jarcode.consoles.computer.ProgramUtils.handleBlockCreate;
import static ca.jarcode.consoles.computer.ProgramUtils.splitArguments;

@ProvidedManual(
		author = "Jarcode",
		version = "1.12",
		contents = "Creates a file at the given path on the filesystem. This program " +
				"will fail when trying to resolve malformed paths, when it encounters a " +
				"sub-folder that does not exist, or if the target folder already exists."
)
public class TouchProgram extends FSProvidedProgram {

	private boolean print = true;

	public TouchProgram() {}
	public TouchProgram(boolean print) {
		this.print = print;
	}

	@Override
	public void run(String str, Computer computer) throws Exception {
		Terminal terminal = computer.getTerminal(this);
		touch(str, terminal);
	}

	public FSFile touch(String str, Terminal terminal) {
		String[] args = splitArguments(str);
		if (args.length == 0 || str.isEmpty()) {
			if (print)
				print("touch [FILE]");
			return null;
		}
		ProgramUtils.PreparedBlock pre = handleBlockCreate(args[0], (s) -> print("touch: " + s), terminal, false);
		if (pre.err != null) return null;
		FSStoredFile file = new FSStoredFile(terminal.getComputer());
		pre.blockParent.contents.put(pre.blockName, file);
		return file;
	}
}
