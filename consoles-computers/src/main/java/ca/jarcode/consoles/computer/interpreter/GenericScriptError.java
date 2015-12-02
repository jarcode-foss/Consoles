package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.ascript.interfaces.ScriptError;

public class GenericScriptError extends ScriptError {

	private final Throwable cause;
	private final String message;

	public GenericScriptError(Throwable cause, String message) {
		this.cause = cause;
		this.message = message;
	}

	public GenericScriptError(Throwable cause) {
		this.cause = cause;
		this.message = null;
	}

	public GenericScriptError(String message) {
		this.cause = null;
		this.message = message;
	}

	public GenericScriptError() {
		this.cause = null;
		this.message = null;
	}

	@Override
	public Throwable underlying() {
		return cause;
	}

	@Override
	public String constructMessage() {
		return message;
	}
}
