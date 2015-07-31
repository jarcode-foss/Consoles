package ca.jarcode.consoles.computer.filesystem;

import ca.jarcode.consoles.computer.Computer;
import sun.misc.IOUtils;

import java.io.*;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Some sort of stored file. Legacy files are stored in memory, standard stored files are read from a file
 * on the disk.
 */
public class FSStoredFile extends FSFile {

	private static final byte ID = 0x08;

	public StoredOutputStream out;
	public volatile boolean locked = false;
	private final Object LOCK = new Object();
	private final Supplier<StoredOutputStream> streamFactory;

	public FSStoredFile(Computer source) {
		super(ID);
		streamFactory = () -> fileStream(source.linkFile(this));
	}
	public FSStoredFile(Computer source, UUID uuid) {
		super(ID, uuid);
		streamFactory = () -> fileStream(source.linkFile(this));
	}
	// legacy
	public FSStoredFile(byte[] data) {
		super(ID);
		streamFactory = this::memoryStream;
		try {
			out = memoryStream();
			out.write(data);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	// legacy
	public FSStoredFile(byte[] data, UUID uuid) {
		super(ID, uuid);
		streamFactory = this::memoryStream;
		try {
			out = memoryStream();
			out.write(data);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	// this works if the file is still locked! However, this kills the old stream.
	@Override
	public OutputStream createOutput() {
		locked = true;
		out.dispose();
		out = streamFactory.get();
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
			data = out.getData();
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
	private StoredOutputStream memoryStream() {
		return new StoredOutputStream() {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			@Override
			public void close() {
				out = null;
				locked = false;
			}
			@Override
			public void write(int b) {
				if (out != null) synchronized (LOCK) {
					out.write(b);
				}
			}
			@Override
			public byte[] getData() {
				return out == null ? new byte[0] : out.toByteArray();
			}
			@Override
			public void dispose() {
				out = null;
			}
		};
	}
	private StoredOutputStream fileStream(File file) {
		try {
			return new StoredOutputStream() {
				public FileOutputStream out = new FileOutputStream(file, true);
				@Override
				public void close() {
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					locked = false;
				}
				@Override
				public void write(int b) {
					synchronized (LOCK) {
						writeUnlocked(b);
					}
				}
				public void writeUnlocked(int b) {
					try {
						out.write(b);
					} catch (IOException e) {
						try {
							out = new FileOutputStream(file, true);
						}
						catch (Exception e1) {
							throw new RuntimeException(e1);
						}
					}
				}
				@SuppressWarnings("NullableProblems")
				@Override
				public void write(byte b[], int off, int len) throws IOException {
					if (b == null) {
						throw new NullPointerException();
					} else if ((off < 0) || (off > b.length) || (len < 0) ||
							((off + len) > b.length) || ((off + len) < 0)) {
						throw new IndexOutOfBoundsException();
					} else if (len == 0) {
						return;
					}
					for (int i = 0 ; i < len ; i++) {
						writeUnlocked(b[off + i]);
					}
				}
				@Override
				public byte[] getData() {
					try (FileInputStream in = new FileInputStream(file)) {
						return IOUtils.readFully(in, -1, false);
					}
					catch (IOException e) {
						e.printStackTrace();
						return new byte[0];
					}
				}
				@Override
				public void dispose() {
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						// open and close file, clears the buffer and data on disk
						FileOutputStream temp = new FileOutputStream(file, false);
						temp.close();
					}
					catch (IOException e) {
						// failed to delete file
						e.printStackTrace();
					}
				}
			};
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	private abstract class StoredOutputStream extends OutputStream {
		public abstract byte[] getData();
		public abstract void dispose();
	}
}
