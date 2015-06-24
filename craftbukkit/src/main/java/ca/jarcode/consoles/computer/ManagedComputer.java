package ca.jarcode.consoles.computer;

import ca.jarcode.consoles.internal.ManagedConsole;

import java.util.UUID;

public class ManagedComputer extends Computer {

	public static final int WIDTH = 3, HEIGHT = 2;

	ManagedComputer(String hostname, UUID owner, ManagedConsole console) {
		super(hostname.toLowerCase(), owner, console);
	}

	public ManagedComputer(String hostname, UUID owner) {
		super(hostname.toLowerCase(), owner, WIDTH, HEIGHT);
		ComputerHandler.getInstance().register(this);
	}

	@Override
	public void destroy(boolean delete) {
		super.destroy(delete);
		ComputerHandler.getInstance().unregister(this, delete);
		save();
	}

	@Override
	public boolean setHostname(String hostname) {
		if (ComputerHandler.getInstance().hostnameTaken(hostname))
			return false;
		String old = getHostname();
		super.setHostname(hostname);
		ComputerData.rename(old, hostname);
		return true;
	}
}
