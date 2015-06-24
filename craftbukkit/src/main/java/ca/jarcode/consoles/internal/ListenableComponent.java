package ca.jarcode.consoles.internal;

import ca.jarcode.consoles.event.ConsoleEvent;
import ca.jarcode.consoles.event.ConsoleEventListener;

/*

An interface for components that can have listeners added to

 */
public interface ListenableComponent<T extends ConsoleComponent, E extends ConsoleEvent<T>> {
	public void addEventListener(ConsoleEventListener<T, E> listener);
}
