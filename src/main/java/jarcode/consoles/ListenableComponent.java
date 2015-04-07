package jarcode.consoles;

import jarcode.consoles.event.ConsoleEvent;
import jarcode.consoles.event.ConsoleEventListener;

public interface ListenableComponent<T extends ConsoleComponent, E extends ConsoleEvent<T>> {
	public void addEventListener(ConsoleEventListener<T, E> listener);
}
