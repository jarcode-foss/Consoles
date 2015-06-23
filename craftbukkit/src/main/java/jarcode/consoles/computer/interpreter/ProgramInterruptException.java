package jarcode.consoles.computer.interpreter;

import org.luaj.vm2.LuaError;

public class ProgramInterruptException extends LuaError {
	public ProgramInterruptException(String reason) {
		super(reason);
	}
}
