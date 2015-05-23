package jarcode.consoles;

/*

An interface for components that allows it to take input from a player.

 */
@FunctionalInterface
public interface InputComponent {
	public void handleInput(String input, String player);
}
