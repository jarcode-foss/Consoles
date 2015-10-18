package ca.jarcode.consoles.computer.boot;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.filesystem.FSFile;
import ca.jarcode.consoles.internal.ConsoleComponent;
import ca.jarcode.consoles.internal.InputComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

// This driver, when running, forwards command block input to the active terminal.
public class PlayerCommandDriver extends Driver {

	private InputStream in;

	public PlayerCommandDriver(FSFile device, Computer computer) {
		super(device, computer);
		in = device.createInput();
	}

	@Override
	public void tick() {
		try {
			ConsoleComponent component = computer.getCurrentComponent();
			if (in.available() > 0) {
				DataInputStream data = new DataInputStream(in);
				String text = data.readUTF();
				String player = data.readUTF(); // ignore name
				if (text.startsWith("^")) {
					Terminal terminal = component instanceof Terminal ? (Terminal) component : null;
					if (text.length() >= 2) {
						char c = text.charAt(1);
						if (c == 'C' || c == 'c') {
							if (terminal != null) {
								Player p = Bukkit.getPlayer(player);
								terminal.sigTerm(p);
							}
						}
						else {
							int i;
							try {
								i = Integer.parseInt(Character.toString(c));
								Player p = Bukkit.getPlayer(player);
								computer.switchView(i, p);
							} catch (Throwable ignored) {}
						}
					}
				}
				else if (component instanceof InputComponent)
					((InputComponent) component).handleInput(text, player);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		try {
			in.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
