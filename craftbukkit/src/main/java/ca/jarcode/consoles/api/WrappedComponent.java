package ca.jarcode.consoles.api;

import ca.jarcode.consoles.internal.ConsoleComponent;

/**
 * Used internally to determine if API components are actual components, or wrapped.
 */
@FunctionalInterface
public interface WrappedComponent {
	/**
	 * Obtain the wrapped component
	 *
	 * @return the underlying {@link ca.jarcode.consoles.internal.ConsoleComponent}
	 */
	public ConsoleComponent underlying();
}
