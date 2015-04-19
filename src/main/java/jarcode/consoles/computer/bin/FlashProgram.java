package jarcode.consoles.computer.bin;

import jarcode.consoles.ConsoleButton;
import jarcode.consoles.Position2D;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

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
