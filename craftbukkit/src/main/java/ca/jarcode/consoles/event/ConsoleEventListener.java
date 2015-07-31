package ca.jarcode.consoles.event;

import ca.jarcode.consoles.internal.ConsoleComponent;

public interface ConsoleEventListener<T extends ConsoleComponent, E extends ConsoleEvent<T>> {
	void actionPerformed(E event);
}
