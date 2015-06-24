package ca.jarcode.consoles.computer.filesystem;

import java.io.*;
import java.util.UUID;

public class FSStoredFile extends FSFile {

	private static final byte ID = 0x01;

	public ByteArrayOutputStream out = closableStream();
	public volatile boolean locked = false;
	private final Object LOCK = new Object();

	public FSStoredFile() {
		super(ID);
	}
	public FSStoredFile(byte[] data) {
		super(ID);
		try {
			out.write(data);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	public FSStoredFile(byte[] data, UUID uuid) {
		super(ID, uuid);
		try {
			out.write(data);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	// this works if the file is still locked! However, the old stream becomes useless.
	@Override
	public OutputStream createOutput() {
		locked = true;
		out = closableStream();
		return out;
	}

	// returns the actual output stream. You can't write to the file from multiple locations, though
	// this is contrary to normal unix file locking behaviour, we just assume strict locking.
	public OutputStream getOutput() {
		if (locked) return null;
		return out;
	}

	@Override
	public int getSize() {
		return 0;
	}

	// captures the data at the given point, allowing data to still be written
	@Override
	public InputStream createInput() {
		byte[] data;
		synchronized (LOCK) {
			data = out.toByteArray();
		}
		return new ByteArrayInputStream(data);
	}

	// you cannot 'release' a file, you can only delete it!
	// this is for devices that need to be mounted/unmounted, like command blocks.
	@Override
	public void release() {}

	@Override
	public boolean locked() {
		return locked;
	}
	private ByteArrayOutputStream closableStream() {
		return new ByteArrayOutputStream() {
			@Override
			public void close() {
				locked = false;
			}
			@Override
			public void write(int b) {
				synchronized (LOCK) {
					super.write(b);
				}
			}
		};
	}
}
