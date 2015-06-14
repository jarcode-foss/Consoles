package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.manual.ProvidedManual;

@ProvidedManual(
		author = "Jarcode",
		version = "1.0",
		contents = "Shows the UUID of the owner that this computer belongs to"
)
public class OwnerProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		print(computer.getOwner().toString());
	}
}
