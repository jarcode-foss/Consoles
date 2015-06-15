package jarcode.consoles.api.computer;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.ComputerHandler;

/**
 * These are safe, static methods that can be used to interface with computers.
 */
public class ComputerManagement {

	/**
	 * Returns whether computers are enabled or not on the server
	 *
	 * @return true if enabled, false if disabled.
	 */
	public static boolean computersAreEnabled() {
		return ComputerHandler.getInstance() != null;
	}

	/**
	 * Returns all active hosts on the server
	 *
	 * @return an array of strings representing all the active computers
	 */
	public static String[] getAllHosts() {
		if (computersAreEnabled()) {
			return ComputerHandler.getInstance().getComputers().stream()
					.map(Computer::getHostname)
					.toArray(String[]::new);
		}
		else return new String[0];
	}

	/**
	 * Destroys the computer associated with the given hostname
	 *
	 * @param host the hostname of the computer to destroy
	 */
	public static void destroyHost(String host) {
		if (computersAreEnabled()) {
			Computer computer = ComputerHandler.getInstance().find(host);
			if (computer == null) return;
			computer.destroy(true);
		}
	}

	/**
	 * Changes a computer's hostname, and moves data files accordingly.
	 *
	 * @param host the hostname of the computer to rename
	 * @param newHost the new hostname for the computer
	 * @return whether renaming the computer was sucessful or not
	 */
	public static boolean renameHost(String host, String newHost) {
		if (computersAreEnabled()) {
			Computer computer = ComputerHandler.getInstance().find(host);
			return computer != null && computer.setHostname(newHost);
		}
		else return false;
	}
}
