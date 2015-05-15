package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.Computer;

import java.util.function.Consumer;

public class LuaComputer {

	private Computer computer;

	public LuaComputer(Computer computer) {
		this.computer = computer;
	}
	public boolean message(String channel, String message) {
		Consumer<String> listener = computer.getMessageListener(channel);
		if (listener != null) {
			listener.accept(message);
			return true;
		}
		else return false;
	}
	public String hostname() {
		return computer.getHostname();
	}
	public String owner() {
		return computer.getOwner().toString();
	}
}
