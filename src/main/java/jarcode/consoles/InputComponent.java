package jarcode.consoles;

@FunctionalInterface
public interface InputComponent {
	public void handleInput(String input, String player);
}
