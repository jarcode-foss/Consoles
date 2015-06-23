package jarcode.consoles.computer.interpreter.libraries;

import jarcode.consoles.api.computer.LibraryCreator;

public class Libraries {
	public static void init() {
		LibraryCreator.link(NetLibrary::new, "net", true);
		LibraryCreator.link(ServerLibrary::new, "server", true);
	}
}
