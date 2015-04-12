package jarcode.consoles.computer.boot;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSFile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

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
			Terminal terminal = computer.getCurrentTerminal();
			if (in.available() > 0) {
				DataInputStream data = new DataInputStream(in);
				String text = data.readUTF();
				data.readUTF(); // ignore name
				if (text.startsWith("^")) {
					if (text.length() >= 2) {
						char c = text.charAt(1);
						if (c == 'C' || c == 'c') {
							if (terminal != null)
								terminal.sigTerm();
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
				else if (terminal != null)
					terminal.write(text);
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
