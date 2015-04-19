package jarcode.consoles.computer.bin;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;

public class OwnerProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		print(computer.getOwner().toString());
	}
}
