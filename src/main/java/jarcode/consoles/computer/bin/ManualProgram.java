package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.manual.ManualEntry;
import jarcode.consoles.computer.manual.ManualManager;
import jarcode.consoles.computer.manual.ProvidedManual;
import org.bukkit.ChatColor;

import java.util.Map;

import static jarcode.consoles.computer.ProgramUtils.*;

@ProvidedManual(
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
			print("usage: man [PROGRAM | FUNCTION]");
			return;
		}
		FSProvidedProgram foundProgram = null;
		boolean foundRoot = false;

		if (!str.contains("/")) {
			FSBlock block = computer.getBlock(str, "/bin");
			if (block instanceof FSProvidedProgram) {
				foundProgram = (FSProvidedProgram) block;
			}
		}
		if (foundProgram == null) {
			FSBlock block = computer.getBlock(str, "/");
			if (block instanceof FSProvidedProgram) {
				foundProgram = (FSProvidedProgram) block;
				foundRoot = true;
			}
		}

		ManualEntry programMatch = foundProgram == null ? null : ManualManager.PROVIDED_MAP.get(foundProgram);

		Map<String, ManualEntry> manuals = ManualManager.manuals();

		ManualEntry functionMatch = manuals.get(str);

		// edge case if the user if bringing up a manual for a program in the drive root,
		// make sure to explicitly say that it is from the drive root.
		if (foundRoot && programMatch != null && !str.startsWith("/"))
			str = "/" + str;

		ManualEntry selection;

		if (functionMatch != null && programMatch != null) {
			println("Found multiple manual entries for '" + str + "':\n");
			println(ChatColor.GRAY + "\t\t(function) " + ChatColor.WHITE + functionMatch.getUsage());
			println(ChatColor.GRAY + "\t\t(program) " + ChatColor.WHITE + str);
			print("\nChoose " + ChatColor.RED + "f" + ChatColor.WHITE + " (function) or "
					+ ChatColor.RED + "p" + ChatColor.WHITE + " (program): ");
			String input = read();
			print(input);
			if (input.trim().toLowerCase().equals("p"))
				selection = programMatch;
			else if (input.trim().toLowerCase().equals("f"))
				selection = functionMatch;
			else {
				print("\ninvalid response.");
				return;
			}
		}
		else if (functionMatch == null && programMatch == null) {
			print("no manual entry for '" + str + "'");
			return;
		}
		else selection = functionMatch != null ? functionMatch : programMatch;

		sleep(150);

		computer.getTerminal(this).clear();

		print(selection.getText(str));
	}
}
