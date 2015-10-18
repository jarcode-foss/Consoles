package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.ProgramUtils;
import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.filesystem.FSBlock;
import ca.jarcode.consoles.computer.filesystem.FSFolder;
import ca.jarcode.consoles.computer.filesystem.FSProvidedProgram;
import ca.jarcode.consoles.computer.manual.ProvidedManual;

import java.util.Arrays;
import java.util.stream.Collectors;

import static ca.jarcode.consoles.computer.ProgramUtils.handleBlockCreate;
import static ca.jarcode.consoles.computer.ProgramUtils.splitArguments;

@ProvidedManual(
		author = "Jarcode",
		version = "1.12-b",
		contents = "Creates a directory at the given path on the filesystem. This program " +
				"will fail when trying to resolve malformed paths, when it encounters a " +
				"sub-folder that does not exist, or if the target folder already exists."
)
public class MakeDirectoryProgram extends FSProvidedProgram {

	private boolean print = true;

	public MakeDirectoryProgram() {}

	@Override
	public void run(String str, Computer computer) throws Exception {
		Terminal terminal = computer.getTerminal(this);
		mkdir(str, terminal);
	}

	public MakeDirectoryProgram(boolean print) {
		this.print = print;
	}

	public FSFolder mkdir(String str, Terminal terminal) {
		String[] args = splitArguments(str);
		if (args.length == 0 || str.isEmpty()) {
			print("mkdir [FOLDER]");
			return null;
		}
		ProgramUtils.PreparedBlock pre = handleBlockCreate(args[0], (s) -> print("mkdir: " + s), terminal, false);
		if (pre.err != null) return null;
		FSFolder fo = new FSFolder();
		pre.blockParent.contents.put(pre.blockName, fo);
		return fo;
	}
}
