package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptError;

public class LuaNError extends ScriptError {

	@Override
	public Throwable underlying() {
		return null;
	}

	@Override
	public String constructMessage() {
		return null;
	}
}
