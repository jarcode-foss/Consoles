package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;

@Manual(
		author = "Jarcode",
		version = "2.1",
		contents = "Appends, prefixes and writes to files on the filesystem. This program " +
				"will fail when trying to resolve malformed paths, when it encounters a " +
				"sub-folder that does not exist, or if the target folder already exists."
)
public class WriteProgram extends FSProvidedProgram {

	public WriteProgram() {
		setExecutable(FSGroup.ALL, true);
	}

	@Override
	public void run(String str, Computer computer) throws Exception {
		String[] args = splitArguments(str);
		HashMap<String, Object> properties = new HashMap<>();
		properties.put("append", false);
		properties.put("prefix", false);
		properties.put("force", false);
		if (args.length == 0) {
			println("Usage: write [FILE] [OPTION]... [CONTENTS]");
			println("Flags (not applicable to device files):");
			println("\t-a\tappends text instead of overwriting");
			println("\t-p\tprefixes text instead of overwriting");
			println("\t-f\tignores file locks");
		}
		args = parseFlags(args, (flag, string) -> {
			switch (flag) {
				case 'a':
					properties.put("append", true);
					break;
				case 'p':
					properties.put("prefix", true);
					break;
				case 'f':
					properties.put("force", true);
					break;
			}
		}, c -> "apf".indexOf(c) == -1);
		if (args.length == 0) {
			print("invalid file");
			return;
		}
		if (args.length == 1) {
			print("no data to write");
			return;
		}
		FSBlock block;
		try {
			block = resolve(args[0]);
		} catch (Throwable e) {
			print("could not open: " + args[0] + "(" + e.getClass() + ")");
			return;
		}
		if (block == null) {
			print(args[0] + ": does not exist");
			return;
		}
		Terminal terminal = computer.getTerminal(this);
		String usr = terminal.getUser();
		if (block.getOwner().equals(usr))
			if (!block.check(FSGroup.OWNER, 'w') || !block.check(FSGroup.OWNER, 'r'))
				print("permission denied");
		else
			if (!block.check(FSGroup.ALL, 'w') || !block.check(FSGroup.ALL, 'r'))
				print("permission denied");
		if (!(block instanceof FSFile)) {
			print("not a file: " + args[0]);
			return;
		}
		String text = args[1];
		FSFile file = (FSFile) block;
		if (file instanceof FSStoredFile) {
			FSStoredFile store = (FSStoredFile) file;
			if ((Boolean) properties.get("prefix")) {
				InputStream is = store.createInput();
				byte[] arr = new byte[is.available()];
				if (is.read(arr) != arr.length) {
					println("error: did not read all expected bytes");
				}
				text += new String(arr, Charset.forName("UTF-8"));
			}
			if ((Boolean) properties.get("force") || file.locked()) {
				print("could not write to file: file is locked (is it open somewhere else?)");
				return;
			}
			OutputStream out = (Boolean) properties.get("force") || (Boolean) properties.get("prefix")
					? file.createOutput() : file.getOutput();
			out.write(text.getBytes(Charset.forName("UTF-8")));
			out.close();
		}
		else {
			OutputStream out = file.createOutput();
			if (out == null) {
				print("could not write to file: file is locked (is it open somewhere else?)");
				return;
			}
			out.write(text.getBytes(Charset.forName("UTF-8")));
			out.close();
		}
	}
}
