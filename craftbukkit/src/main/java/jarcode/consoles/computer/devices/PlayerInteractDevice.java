package jarcode.consoles.computer.devices;

import jarcode.consoles.util.Position2D;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.LinkedStream;
import jarcode.consoles.computer.filesystem.FSFile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class PlayerInteractDevice extends FSFile {

	private final Object LOCK = new Object();
	private List<OutputStream> outputs = new ArrayList<>();
	private BiConsumer<String, Position2D> consumer;
	private Computer computer;

	public PlayerInteractDevice(Computer computer) {
		// device id
		super((byte) 0x03);
		this.computer = computer;
		synchronized (LOCK) {
			consumer = (player, pos) -> {
				synchronized (LOCK) {
					for (OutputStream out : outputs) {
						try {
							DataOutputStream data = new DataOutputStream(out);
							data.writeShort(pos.getX());
							data.writeShort(pos.getY());
							data.writeUTF(player);
						}
						// shouldn't happen
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			};
			computer.registerClickListener(consumer);
		}
	}

	// makes no sense to write to a player device
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
			computer.unregisterClickListener(consumer);
		}
	}

	// this device doesn't lock! Anything can read from it.
	@Override
	public boolean locked() {
		return false;
	}
}
