package jarcode.consoles.api;

import jarcode.consoles.ConsoleComponent;

/**
 * Used internally to determine if API components are actual components, or wrapped.
 */
@FunctionalInterface
public interface WrappedComponent {
	/**
	 * Obtain the wrapped component
	 *
	 * @return the underlying {@link jarcode.consoles.ConsoleComponent}
	 */
	public ConsoleComponent underlying();
}
