package ca.jarcode.consoles.computer.interpreter.libraries;

import ca.jarcode.consoles.api.computer.LibraryCreator;

public class Libraries {
	public static void init() {
		LibraryCreator.link(NetLibrary::new, "net", true);
		LibraryCreator.link(ServerLibrary::new, "server", true);
	}
}
