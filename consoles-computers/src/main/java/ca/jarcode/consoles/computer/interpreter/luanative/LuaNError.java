package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptError;

@SuppressWarnings("unused")
public class LuaNError extends ScriptError {

	private String message;

	public LuaNError(String message) {
		this.message = message;
	}

	@Override
	public Throwable underlying() {
		return null;
	}

	@Override
	public String constructMessage() {
		return message;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
