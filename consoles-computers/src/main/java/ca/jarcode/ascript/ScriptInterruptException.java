package ca.jarcode.ascript;

import org.luaj.vm2.LuaError;

public class ScriptInterruptException extends LuaError {
	public ScriptInterruptException(String reason) {
		super(reason);
	}
}
