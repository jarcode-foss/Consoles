package jarcode.consoles.computer.devices;

import jarcode.consoles.computer.filesystem.FSFile;

import java.io.InputStream;
import java.io.OutputStream;

public class PlayerCommandDevice extends FSFile {
	public PlayerCommandDevice() {
		super((byte) 0x03);
	}

	@Override
	public OutputStream createOutput() {
		return null;
	}

	@Override
	public OutputStream getOutput() {
		return null;
	}

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public InputStream createInput() {
		return null;
	}

	@Override
	public void release() {

	}

	@Override
	public boolean locked() {
		return false;
	}
}
