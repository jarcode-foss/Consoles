package ca.jarcode.consoles.computer.hooks;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.ComputerHandler;

/**
 * These are safe, static methods that can be used to interface with computers.
 */
public class ComputerManagement {

	/**
	 * Returns all active hosts on the server
	 *
	 * @return an array of strings representing all the active computers
	 */
	public static String[] getAllHosts() {
		return ComputerHandler.getInstance().getComputers().stream()
				.map(Computer::getHostname)
				.toArray(String[]::new);
	}

	/**
	 * Destroys the computer associated with the given hostname
	 *
	 * @param host the hostname of the computer to destroy
	 */
	public static void destroyHost(String host) {
		Computer computer = ComputerHandler.getInstance().find(host);
		if (computer == null) return;
		computer.destroy(true);
	}

	/**
	 * Changes a computer's hostname, and moves data files accordingly.
	 *
	 * @param host the hostname of the computer to rename
	 * @param newHost the new hostname for the computer
	 * @return whether renaming the computer was sucessful or not
	 */
	public static boolean renameHost(String host, String newHost) {
		Computer computer = ComputerHandler.getInstance().find(host);
		return computer != null && computer.setHostname(newHost);
	}
}
