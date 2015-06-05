package jarcode.consoles.computer.interpreter.libraries;

import jarcode.consoles.api.computer.LibraryCreator;

public class Libraries {
	public static void init() {
		LibraryCreator.link(new NetLibrary(), "net", true);
	}
}
