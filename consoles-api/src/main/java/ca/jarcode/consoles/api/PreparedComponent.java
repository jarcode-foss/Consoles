package ca.jarcode.consoles.api;

/**
 * Represents a component that is not actually created until it has a
 * canvas to build against.
 */
@FunctionalInterface
public interface PreparedComponent {
	/**
	 * Prepares and builds the underlying component
	 *
	 * @param renderer the renderer to build against.
	 */
	void prepare(Canvas renderer);
}
