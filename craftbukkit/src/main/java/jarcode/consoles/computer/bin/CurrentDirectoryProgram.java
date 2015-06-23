package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.filesystem.FSGroup;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.manual.ProvidedManual;

@ProvidedManual(
		author = "Jarcode",
		version = "1.1",
		contents = "Opens a dialog that presents the option to delete the computer. " +
				"Only the owner of this computer and authorize deletion. All data is " +
				"lost when the computer is deleted."
)
public class CurrentDirectoryProgram extends FSProvidedProgram {

	public CurrentDirectoryProgram() {
		setExecutable(FSGroup.ALL, true);
	}
	@Override
	public void run(String str, Computer computer) throws Exception {
		str = handleEncapsulation(str.trim());
		if (str.isEmpty()) {
			return;
		}
		Terminal terminal = computer.getTerminal(this);
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block == null)
			println("cd: " + str + ": No such file or directory");
		else if (block instanceof FSFolder) {
			if (str.startsWith("/"))
				terminal.setCurrentDirectory(str);
			else if (terminal.getCurrentDirectory().endsWith("/"))
				terminal.setCurrentDirectory(terminal.getCurrentDirectory() + str);
			else
				terminal.setCurrentDirectory(terminal.getCurrentDirectory() + "/" + str);
		}
		else
			println("cd: " + str + ": No such file or directory");
	}
}
