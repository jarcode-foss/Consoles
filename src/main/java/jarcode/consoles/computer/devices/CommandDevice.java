package jarcode.consoles.computer.devices;

import jarcode.consoles.ConsoleHandler;
import jarcode.consoles.ConsoleListener;
import jarcode.consoles.computer.LinkedStream;
import jarcode.consoles.computer.filesystem.FSFile;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CommandDevice extends FSFile {

	public CommandBlock block;
	private final Object LOCK = new Object();
	private List<OutputStream> outputs = new ArrayList<>();

	// Setup the device
	public CommandDevice(CommandBlock block) {
		// device id
		super((byte) 0x03);
		synchronized (LOCK) {
			if (ConsoleHandler.isRegistered(block)) {
				ConsoleHandler.registerListener(block, new ConsoleListener() {
					@Override
					public String execute(CommandSender sender, String text) {
						synchronized (LOCK) {
							for (OutputStream out : outputs) {
								try {
									out.write(text.getBytes(Charset.forName("UTF-8")));
								}
								// shouldn't happen
								catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						return "Sent to computer";
					}
				});
			} else throw new IllegalArgumentException("Command block already registered!");
			this.block = block;
		}
	}

	// Command blocks have no input handling atm, I'll add the ability to write it it later (for redstone I/O)
	@Override
	public OutputStream createOutput() {
		return new OutputStream() {
			@Override
			public void write(int ignored) {}
		};
	}

	// same as above
	@Override
	public OutputStream getOutput() {
		return new OutputStream() {
			@Override
			public void write(int ignored) {}
		};
	}

	// it's a device, it has no data.
	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public InputStream createInput() {
		synchronized (LOCK) {
			LinkedStream stream = new LinkedStream();
			final OutputStream out = stream.createOutput();
			outputs.add(out);
			stream.registerCloseListener(() -> {
				synchronized (LOCK) {
					outputs.remove(out);
				}
			});
			return stream;
		}
	}

	// when this device is un mounted, we need to fix the command block that is attached
	@Override
	public void release() {
		synchronized (LOCK) {
			ConsoleHandler.restoreCommandBlock(block);
		}
	}

	// this device doesn't lock! Anything can read from it.
	@Override
	public boolean locked() {
		return false;
	}
}
