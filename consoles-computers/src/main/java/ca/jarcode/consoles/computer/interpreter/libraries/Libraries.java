package ca.jarcode.consoles.computer.interpreter.libraries;

import ca.jarcode.ascript.LibraryCreator;

public class Libraries {
	public static void init() {
		LibraryCreator.link(NetLibrary::new, "net", true);
		LibraryCreator.link(ServerLibrary::new, "server", true);
	}
}
