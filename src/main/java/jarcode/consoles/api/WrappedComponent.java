package jarcode.consoles.api;

import jarcode.consoles.internal.ConsoleComponent;

/**
 * Used internally to determine if API components are actual components, or wrapped.
 */
@FunctionalInterface
public interface WrappedComponent {
	/**
	 * Obtain the wrapped component
	 *
	 * @return the underlying {@link jarcode.consoles.internal.ConsoleComponent}
	 */
	public ConsoleComponent underlying();
}
