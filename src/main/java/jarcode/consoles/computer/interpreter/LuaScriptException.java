package jarcode.consoles.computer.interpreter;

public class LuaScriptException extends RuntimeException {

	public LuaScriptException() {
		super();
	}

	public LuaScriptException(String reason, Throwable cause) {
		super(reason, cause);
	}
	public LuaScriptException(String reason) {
		super(reason);
	}
}
