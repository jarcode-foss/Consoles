package jarcode.consoles.computer.interpreter;

public class ProgramInterruptException extends LuaScriptException {
	public ProgramInterruptException(String reason, Throwable cause) {
		super(reason, cause);
	}
	public ProgramInterruptException(String reason) {
		super(reason);
	}
}
