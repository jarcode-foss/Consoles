package jarcode.consoles.computer.filesystem;

import java.io.FileNotFoundException;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("SpellCheckingInspection")
public class FSFolder extends FSBlock {

	private static final byte ID = 0x00;

	// These are mappings to FSBlock objects and are allowed to contain duplicate references anywhere in the filesystem
	// Removing from these will cause the object to 'technically' be deleted from the filesystem, having no reference
	// from the root tree, but java still needs to clean it up.
	// this is sychronized because of the possibility of changes from multiple threads at once
	public ConcurrentHashMap<String, FSBlock> contents = null;

	public FSFolder() {
		super(ID);
		this.contents = new ConcurrentHashMap<>();
	}
	public FSBlock get(String path) throws FileNotFoundException {
		if (path.trim().equals("/") || path.trim().isEmpty())
			return this;
		String sub = section(path, "/")[0];
		FSBlock block = contents.get(sub);
		if (block == null)
			throw new FileNotFoundException("'" + sub + "' (" + path + ")");
		String remaining = sub.length() == path.length() ? null : path.substring(sub.length() + 1);
		if (remaining != null && !(block instanceof FSFolder)) {
			throw new FileNotFoundException(sub + " is a file or program");
		}
		else if (remaining != null) {
			return ((FSFolder) block).get(remaining);
		}
		else return block;
	}
	public boolean exists(String path) {
		if (path == null) return false;
		String sub = section(path, "/")[0];
		FSBlock block = contents.get(sub);
		if (block == null)
			return false;
		String remaining = sub.length() == path.length() ? null : path.substring(sub.length() + 1);
		return remaining == null // if null, that means this is the end of the path and this was the final node
				|| (block instanceof FSFolder && ((FSFolder) block).exists(remaining));
	}
	public void mk(String path, String name, FSBlock block) throws FileNotFoundException {
		FSBlock at = get(path);
		if (!(at instanceof FSFolder)) {
			throw new FileNotFoundException(path  + " is a file or program");
		}
		((FSFolder) at).contents.put(name, block);
	}
	public void mkr(String path, String name, FSBlock block) throws FileNotFoundException {
		if (!mkdir(path))
			throw new FileNotFoundException("Invalid path: " + path);
		FSBlock at = get(path);
		if (!(at instanceof FSFolder)) {
			throw new FileNotFoundException(path  + " is a file or program");
		}
		((FSFolder) at).contents.put(name, block);
	}
	@SuppressWarnings("SimplifiableIfStatement")
	public boolean mkdir(String path) {
		String sub = section(path, "/")[0];
		FSBlock at = contents.get(sub);
		String remaining = sub.length() == path.length() ? sub : path.substring(sub.length() + 1);
		if (at == null && remaining.length() > 0)
			contents.put(sub, new FSFolder());
		if (remaining.length() > 0 && !(at instanceof FSFolder)) {
			return false;
		}
		else if (remaining.length() > 0 && at instanceof FSFolder) {
			return ((FSFolder) at).mkdir(remaining);
		}
		else return true;
	}
}
