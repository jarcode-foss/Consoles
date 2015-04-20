package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

@Manual(
		author = "Jarcode",
		version = "1.19",
		contents = "Removes a file or folder at the given path on the filesystem. This program " +
				"will fail when trying to resolve malformed paths, when it encounters a " +
				"sub-folder that does not exist, or if the target folder already exists."
)
public class RemoveProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {

		String[] args = splitArguments(str);
		HashMap<String, Object> properties = new HashMap<>();
		properties.put("recursive", false);
		if (args.length == 0 || str.trim().isEmpty()) {
			printUsage();
			return;
		}
		args = parseFlags(args, (flag, string) -> {
			switch (flag) {
				case 'r':
					properties.put("recursive", true);
					break;
			}
		}, c -> "r".indexOf(c) == -1);
		if (args.length == 0) {
			printUsage();
			return;
		}
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
		if (f.trim().isEmpty() && str.startsWith("/"))
			f = "/";
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
	private void printUsage() {
		println("Usage: rm [OPTION]... [FILE/FOLDER] ");
		println("Flags:");
		print("\t-r\tremoves files recursively");
	}
}
