package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.filesystem.*;
import ca.jarcode.consoles.computer.manual.ProvidedManual;

import java.util.Arrays;
import java.util.stream.Collectors;

import static ca.jarcode.consoles.computer.ProgramUtils.*;

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
		touch(str, computer, terminal);
	}

	public FSFile touch(String str, Computer computer, Terminal terminal) {
		String[] args = splitArguments(str);
		if (args.length == 0 || str.isEmpty()) {
			if (print)
				print("touch [FILE]");
			return null;
		}
		str = args[0];
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block != null) {
			if (print)
				print("touch: " + str.trim() + ": file or folder exists");
			return null;
		}
		block = computer.getBlock("", terminal.getCurrentDirectory());
		if (!(block instanceof FSFolder)) {
			if (print)
				print("touch: " + str.trim() + ": invalid current directory");
			return null;
		}
		String[] arr = FSBlock.section(str, "/");
		String f = Arrays.asList(arr).stream()
				.limit(arr.length == 0 ? 0 : arr.length - 1)
				.collect(Collectors.joining("/"));
		if (f.trim().isEmpty() && str.startsWith("/"))
			f = "/";
		String n = Arrays.asList(arr).stream()
				.filter(s -> !s.isEmpty())
				.reduce((o1, o2) -> o2)
				.get();
		if (!FSBlock.allowedBlockName(n)) {
			if (print)
				print("touch: " + n.trim() + ": bad block name");
			return null;
		}
		FSBlock folder = computer.getBlock(f, terminal.getCurrentDirectory());
		if (folder == null) {
			if (print)
				print("touch: " + f.trim() + ": does not exist");
			return null;
		}
		if (!(folder instanceof FSFolder)) {
			if (print)
				print("touch: " + f.trim() + ": not a folder");
			return null;
		}
		FSStoredFile file = new FSStoredFile();
		((FSFolder) folder).contents.put(n, file);
		return file;
	}
}
