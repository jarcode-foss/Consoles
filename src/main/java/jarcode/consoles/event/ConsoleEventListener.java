package jarcode.consoles.event;

import jarcode.consoles.internal.ConsoleComponent;

public interface ConsoleEventListener<T extends ConsoleComponent, E extends ConsoleEvent<T>> {
	public void actionPerformed(E event);
}
