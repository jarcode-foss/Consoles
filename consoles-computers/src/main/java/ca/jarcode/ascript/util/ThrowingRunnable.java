package ca.jarcode.ascript.util;

@FunctionalInterface
public interface ThrowingRunnable<E extends Throwable> {
	void run() throws E;
}
