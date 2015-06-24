package ca.jarcode.consoles.computer.devices;

import ca.jarcode.consoles.computer.filesystem.FSFile;

import java.io.InputStream;
import java.io.OutputStream;

public class NullDevice extends FSFile {

	public NullDevice() {
		// device id
		super((byte) 0x03);
	}

	// do not store/handle data
	@Override
	public OutputStream createOutput() {
		return new OutputStream() {
			@Override
			public void write(int ignored) {}
		};
	}

	// null devices don't give a shit about input
	@Override
	public OutputStream getOutput() {
		return new OutputStream() {
			@Override
			public void write(int ignored) {}
		};
	}

	// IT HAS NO SIZE! ARGHGHHHHH
	@SuppressWarnings("SpellCheckingInspection")
	@Override
	public int getSize() {
		return 0;
	}

	// return -1 (EOF), as per *nix /dev/null behaviour
	@Override
	public InputStream createInput() {
		return new InputStream() {
			@Override
			public int read() {
				return -1;
			}
		};
	}

	// nothing to unmount for a null device.
	@Override
	public void release() {}

	@Override
	public boolean locked() {
		return false;
	}
}
