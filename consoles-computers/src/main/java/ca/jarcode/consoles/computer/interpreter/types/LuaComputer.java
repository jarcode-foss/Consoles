package ca.jarcode.consoles.computer.interpreter.types;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.manual.Arg;
import ca.jarcode.consoles.computer.manual.FunctionManual;
import ca.jarcode.consoles.computer.manual.TypeManual;

import java.util.function.Consumer;

@TypeManual("Represents a computer that exists in the server.")
@SuppressWarnings("unused")
public class LuaComputer {

	private Computer computer;

	public LuaComputer(Computer computer) {
		this.computer = computer;
	}

	@FunctionManual("Sends a raw message to this computer on the given channel.")
	public boolean message(
			@Arg(name = "channel", info = "the name of the channel to use") String channel,
			@Arg(name = "message", info = "the contents of the message to send") String message) {
		Consumer<String> listener = computer.getMessageListener(channel);
		if (listener != null) {
			listener.accept(message);
			return true;
		}
		else return false;
	}

	@FunctionManual("Returns the hostname of this computer as a string.")
	public String hostname() {
		return computer.getHostname();
	}

	@FunctionManual("Returns the owner of this computer, as a UUID string.")
	public String owner() {
		return computer.getOwner().toString();
	}
}
