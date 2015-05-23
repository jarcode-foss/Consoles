package jarcode.consoles;

import jarcode.consoles.event.ConsoleEvent;
import jarcode.consoles.event.ConsoleEventListener;

/*

An interface for components that can have listeners added to

 */
public interface ListenableComponent<T extends ConsoleComponent, E extends ConsoleEvent<T>> {
	public void addEventListener(ConsoleEventListener<T, E> listener);
}
