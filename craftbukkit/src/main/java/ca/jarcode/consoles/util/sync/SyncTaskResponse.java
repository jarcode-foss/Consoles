package ca.jarcode.consoles.util.sync;

// interface with code ran in a scheduler
@FunctionalInterface
public interface SyncTaskResponse<T> {
	public void run(T in);
}
