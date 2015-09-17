package ca.jarcode.consoles.computer.interpreter;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;
import java.util.Map;

public class InstanceLib extends TwoArgFunction {

	private HashMap<String, LibFunction> table;

	public InstanceLib(HashMap<String, LibFunction> table) {
		this.table = table;
	}

	@Override
	public LuaValue call(LuaValue ignored, LuaValue env) {
		for (Map.Entry<String, LibFunction> entry : Lua.staticFunctions.entrySet()) {
			env.set(entry.getKey(), entry.getValue());
		}
		return env;
	}
}
