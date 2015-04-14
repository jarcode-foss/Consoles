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
	@Override
	public void run(String str, Computer computer) throws Exception {
		String[] args = splitArguments(str);
		if (args.length == 0 || str.isEmpty()) {
			print("mkdir [FOLDER]");
			return;
		}
		str = args[0];
		Terminal terminal = computer.getTerminal(this);
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block != null) {
			print("mkdir: " + str.trim() + ": file or folder exists");
			return;
		}
		block = resolve("");
		if (!(block instanceof FSFolder)) {
			print("mkdir: " + str.trim() + ": invalid current directory");
			return;
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
			print("mkdir: " + n.trim() + ": bad block name");
			return;
		}
		FSBlock folder = computer.getBlock(f, terminal.getCurrentDirectory());
		if (folder == null) {
			print("mkdir: " + f.trim() + ": does not exist");
			return;
		}
		if (!(folder instanceof FSFolder)) {
			print("mkdir: " + f.trim() + ": not a folder");
			return;
		}
		((FSFolder) folder).contents.put(n, new FSFolder());
	}
}
