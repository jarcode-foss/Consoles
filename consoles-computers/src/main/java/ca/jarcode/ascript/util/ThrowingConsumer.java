package ca.jarcode.ascript.util;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
	void accept(T obj) throws E;
}
