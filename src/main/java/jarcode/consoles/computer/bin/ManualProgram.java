package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import org.bukkit.ChatColor;

import java.lang.annotation.Annotation;
import java.util.Arrays;

@Manual(
		author = "Jarcode",
		version = "1.1",
		contents = "A program that queries information from provided applications " +
				"that are installed with a computer."
)
public class ManualProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		str = str.trim();
		if (str.isEmpty()) {
			print("usage: man [PROGRAM]");
			return;
		}
		if (!str.contains("/")) {
			FSBlock block = computer.getBlock(str, "/bin");
			if (block instanceof FSProvidedProgram) {
				Class type = block.getClass();
				Annotation[] arr = type.getAnnotations();
				Manual man = (Manual) Arrays.asList(arr).stream()
						.filter(a -> Manual.class.isAssignableFrom(a.annotationType()))
						.findFirst()
						.orElseGet(() -> null);
				if (man != null) {
					computer.getTerminal(this).clear();
					println("\t\t\t\t\t\t\t\tManual for " + ChatColor.GREEN + str + "\n");
					println(ChatColor.GREEN + "AUTHOR: " + ChatColor.WHITE + man.author());
					nextln();
					println(ChatColor.GREEN + "VERSION: " + ChatColor.WHITE + man.version());
					nextln();
					println(ChatColor.GREEN + "SYNOPSIS: " + ChatColor.WHITE + man.contents());
					return;
				}
			}
		}
		print("no manual entry for '" + str + "'");
	}
}
