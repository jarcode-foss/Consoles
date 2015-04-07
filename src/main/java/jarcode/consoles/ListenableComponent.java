package jarcode.consoles;

public interface ListenableComponent<T extends ConsoleComponent, E extends ConsoleEvent<T>> {
	public void addEventListener(ConsoleEventListener<T, E> listener);
}
