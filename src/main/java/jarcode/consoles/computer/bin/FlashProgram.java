package jarcode.consoles.computer.bin;

import jarcode.consoles.ConsoleButton;
import jarcode.consoles.util.Position2D;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

@Manual(
		author = "Jarcode",
		version = "1.1",
		contents = "Restores both Lua and Java programs that were modified or removed " +
				"since the computer was created. This also does the same for files in " +
				"/lib."
)
public class FlashProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		schedule(() -> {
			ConsoleButton delete = new ConsoleButton(computer.getConsole(), "Restore");
			ConsoleButton deny = new ConsoleButton(computer.getConsole(), "Quit");
			Position2D pos = computer.dialog("This program restores files in /bin and /lib", delete, deny);
			delete.addEventListener(event -> {
				computer.getKernel().flashPrograms();
				computer.getConsole().removeComponent(pos);
			});
			deny.addEventListener(event -> computer.getConsole().removeComponent(pos));
		});
	}
}
