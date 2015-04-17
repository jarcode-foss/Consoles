package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.filesystem.FSStoredFile;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MakeDirectoryProgram extends FSProvidedProgram {

	private boolean print = true;

	public MakeDirectoryProgram() {}

	@Override
	public void run(String str, Computer computer) throws Exception {
		Terminal terminal = computer.getTerminal(this);
		mkdir(str, computer, terminal);
	}

	public MakeDirectoryProgram(boolean print) {
		this.print = print;
	}

	public FSFolder mkdir(String str, Computer computer, Terminal terminal) {
		String[] args = splitArguments(str);
		if (args.length == 0 || str.isEmpty()) {
			print("mkdir [FOLDER]");
			return null;
		}
		str = args[0];
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block != null) {
			if (print)
				print("mkdir: " + str.trim() + ": file or folder exists");
			return null;
		}
		block = resolve("");
		if (!(block instanceof FSFolder)) {
			if (print)
				print("mkdir: " + str.trim() + ": invalid current directory");
			return null;
		}
		String[] arr = FSBlock.section(str, "/");
		String f = Arrays.asList(arr).stream()
				.limit(arr.length == 0 ? 0 : arr.length - 1)
				.collect(Collectors.joining("/"));
		String n = Arrays.asList(arr).stream()
				.filter(s -> !s.isEmpty())
				.reduce((o1, o2) -> o2)
				.get();
		if (!FSBlock.allowedBlockName(n)) {
			if (print)
				print("mkdir: " + n.trim() + ": bad block name");
			return null;
		}
		FSBlock folder = computer.getBlock(f, terminal.getCurrentDirectory());
		if (folder == null) {
			if (print)
				print("mkdir: " + f.trim() + ": does not exist");
			return null;
		}
		if (!(folder instanceof FSFolder)) {
			if (print)
				print("mkdir: " + f.trim() + ": not a folder");
			return null;
		}
		FSFolder fo = new FSFolder();
		((FSFolder) folder).contents.put(n, fo);
		return fo;
	}
}
