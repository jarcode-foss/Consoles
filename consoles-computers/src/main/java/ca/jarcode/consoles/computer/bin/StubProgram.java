package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.filesystem.FSProvidedProgram;

public class StubProgram extends FSProvidedProgram {

	private final String response;

	public StubProgram(String response) {
		this.response = response;
	}

	@Override
	public void run(String str, Computer computer) throws Exception {
		print(response);
	}
}
