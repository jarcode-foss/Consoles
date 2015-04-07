package jarcode.consoles.util.sync;

// interface with code ran in a separate thread
@FunctionalInterface
public interface SyncTask<T> {
	public T run();
}
