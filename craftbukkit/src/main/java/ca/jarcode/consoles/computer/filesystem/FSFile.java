package ca.jarcode.consoles.computer.filesystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public abstract class FSFile extends FSBlock {

	public FSFile(byte id) {
		super(id);
	}
	public FSFile(byte id, UUID uuid) {
		super(id, uuid);
	}

	public abstract OutputStream createOutput(); // new, null if locked (and can't write)
	public abstract OutputStream getOutput(); // current, null if locked
	public abstract int getSize(); // a number representing the size of the file on the filesystem
	public abstract InputStream createInput(); // should be new, null if locked
	public abstract void release(); // basically unmount the file
	public abstract boolean locked(); // whether IO is currently locked
}
