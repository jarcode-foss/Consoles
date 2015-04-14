package jarcode.consoles.computer.bin;

import com.google.common.base.Joiner;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.filesystem.FSStoredFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class RemoveProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {

		String[] args = splitArguments(str);
		HashMap<String, Object> properties = new HashMap<>();
		properties.put("recursive", false);
		if (args.length == 0 || str.trim().isEmpty()) {
			println("Usage: rm [OPTION]... [FILE/FOLDER] ");
			println("Flags:");
			print("\t-r\tremoves files recursively");
			return;
		}
		args = parseFlags(args, (flag, string) -> {
			switch (flag) {
				case 'r':
					properties.put("recursive", true);
					break;
			}
		}, c -> "r".indexOf(c) == -1);
		str = args[0];
		Terminal terminal = computer.getTerminal(this);
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block == null) {
			print("rm: " + str.trim() + ": file does not exist");
			return;
		}
		block = resolve("");
		if (!(block instanceof FSFolder)) {
			print("rm: " + str.trim() + ": invalid current directory");
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
			print("rm: " + n.trim() + ": bad block name");
			return;
		}
		FSBlock folder = computer.getBlock(f, terminal.getCurrentDirectory());
		if (folder == null) {
			print("rm: " + f.trim() + ": does not exist");
			return;
		}
		if (!(folder instanceof FSFolder)) {
			print("rm: " + f.trim() + ": not a folder");
			return;
		}
		FSFolder b = ((FSFolder) folder);
		FSBlock a = b.contents.get(n);
		if (!((Boolean) properties.get("recursive")) && a instanceof FSFolder && ((FSFolder) a).contents.size() > 0) {
			print("rm: " + str.trim() + ": folder is not empty");
			return;
		}
		b.contents.remove(n);
	}
}
