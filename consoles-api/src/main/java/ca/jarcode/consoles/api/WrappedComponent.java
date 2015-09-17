package ca.jarcode.consoles.api;

/**
 * Used internally to determine if API components are actual components, or wrapped.
 */
@FunctionalInterface
public interface WrappedComponent {
	/**
	 * Obtain the wrapped component
	 *
	 * @return the underlying internal component
	 */
	Object underlying();
}
