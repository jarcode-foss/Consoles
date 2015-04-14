package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import org.bukkit.ChatColor;

import java.util.stream.Collectors;

public class HelpProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		println("Installed programs: ");
		FSBlock block = resolve("/bin");
		if (block instanceof FSFolder) {
			println(((FSFolder) block).contents.entrySet().stream()
					.filter(entry -> entry.getValue() instanceof FSProvidedProgram
							|| entry.getValue() instanceof FSFile)
					.map(entry -> (entry.getValue() instanceof FSProvidedProgram ?
							ChatColor.RED : ChatColor.YELLOW) + entry.getKey() + ChatColor.RESET)
					.collect(Collectors.joining("\t")));
		}
		print("");
		print("Visit " + ChatColor.BLUE+ "github.com/wacossusca34/Consoles/"
				+ ChatColor.WHITE + " for source code & support.");
	}
}
