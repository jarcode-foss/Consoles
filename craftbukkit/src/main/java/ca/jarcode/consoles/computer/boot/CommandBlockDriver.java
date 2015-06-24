package ca.jarcode.consoles.computer.boot;

import ca.jarcode.consoles.internal.ConsoleComponent;
import ca.jarcode.consoles.internal.InputComponent;
import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.filesystem.FSFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

// This driver, when running, forwards command block input to the active terminal.
public class CommandBlockDriver extends Driver {

	private final InputStream in;

	public CommandBlockDriver(FSFile device, Computer computer) {
		super(device, computer);
		in = device.createInput();
	}

	@Override
	public void tick() {
		try {
			ConsoleComponent component = computer.getCurrentComponent();
			if (in.available() > 0) {
				byte[] data = new byte[in.available()];
				if (in.read(data) != data.length)
					throw new IOException("Invalid array length on driver tick");
				String text = new String(data, Charset.forName("UTF-8"));

				if (text.startsWith("^")) {
					Terminal terminal = component instanceof Terminal ? (Terminal) component : null;
					if (text.length() >= 2) {
						char c = text.charAt(1);
						if (c == 'C' || c == 'c') {
							if (terminal != null) {
								terminal.sigTerm(null);
							}
						}
						else {
							int i;
							try {
								i = Integer.parseInt(Character.toString(c));
								computer.switchView(i);
							} catch (Throwable ignored) {}
						}
					}
				}
				else if (component instanceof InputComponent)
					((InputComponent) component).handleInput(text, null);
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
