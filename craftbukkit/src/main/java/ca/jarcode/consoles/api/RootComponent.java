package ca.jarcode.consoles.api;

import ca.jarcode.consoles.internal.ConsoleRenderer;

/**
 * Represents a component that handles placement in the root container (console) itself
 */
@FunctionalInterface
public interface RootComponent {
	/**
	 * Handles placement of this component
	 *
	 * @param renderer the renderer to add to.
	 */
	void place(ConsoleRenderer renderer);
}
