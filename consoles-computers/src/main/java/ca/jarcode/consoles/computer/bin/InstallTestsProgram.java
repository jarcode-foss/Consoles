package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.api.Position2D;
import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.filesystem.FSProvidedProgram;
import ca.jarcode.consoles.computer.interpreter.LuaDefaults;
import ca.jarcode.consoles.internal.ConsoleButton;

import static ca.jarcode.consoles.computer.ProgramUtils.schedule;

public class InstallTestsProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		schedule(() -> {
			ConsoleButton install = new ConsoleButton(computer.getConsole(), "Install");
			ConsoleButton deny = new ConsoleButton(computer.getConsole(), "Quit");
			Position2D pos = computer.dialog("This program installs test programs into /bin/tests", install, deny);
			install.addEventListener(event -> LuaDefaults.loadTests(computer));
			deny.addEventListener(event -> computer.getConsole().removeComponent(pos));
		});
	}
}
