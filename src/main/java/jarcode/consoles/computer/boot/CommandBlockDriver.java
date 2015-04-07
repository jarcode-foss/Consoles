package jarcode.consoles.computer.boot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

// This driver, when running, forwards command block input to the active terminal.
public class CommandBlockDriver extends Driver<CommandDevice> {

	private InputStream in;

	public CommandBlockDriver(CommandDevice device, Computer computer) {
		super(device, computer);
		in = device.createInput();
	}

	@Override
	public void tick() {
		try {
			Terminal terminal = computer.getCurrentTerminal();
			if (terminal != null && in.available() > 0) {
				byte[] data = new byte[in.available()];
				if (in.read(data) != data.length)
					throw new IOException("Invalid array length on driver tick");
				terminal.write(new String(data, Charset.forName("UTF-8")));
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
