package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.filesystem.FSProvidedProgram;
import ca.jarcode.consoles.computer.manual.ProvidedManual;

@ProvidedManual(
		author = "Jarcode",
		version = "1.0",
		contents = "Shows the UUID of the owner that this computer belongs to"
)
@Deprecated
public class OwnerProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		print(computer.getOwner().toString());
	}
}
