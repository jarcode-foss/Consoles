package jarcode.consoles.event;

import jarcode.consoles.ConsoleComponent;

public class ConsoleEvent<T extends ConsoleComponent> {

	private T context;

	public ConsoleEvent(T context) {
		this.context = context;
	}

	public T getContext() {
		return context;
	}
}
