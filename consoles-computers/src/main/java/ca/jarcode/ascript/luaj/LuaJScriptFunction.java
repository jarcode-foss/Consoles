package ca.jarcode.ascript.luaj;

import ca.jarcode.ascript.interfaces.ScriptFunction;
import ca.jarcode.ascript.interfaces.ScriptValue;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.lib.LibFunction;

public class LuaJScriptFunction implements ScriptFunction {

	public final LuaFunction func;

	public LuaJScriptFunction(LuaFunction func) {
		this.func = func;
	}

	@Override
	public ScriptValue call(ScriptValue... args) {
		try {
			if (func instanceof LibFunction) {
				return handle((LibFunction) func, args);
			} else return handle(func, args);
		}
		catch (LuaError e) {
			throw new LuaJError(e);
		}
	}

	@Override
	public ScriptValue getAsValue() {
		return new LuaJScriptValue(func);
	}

	@Override
	public void release() {}

	public static ScriptValue handle(LuaFunction func, ScriptValue[] args) {
		switch (args.length) {
			case 0: return new LuaJScriptValue(func.call());
			case 1: return new LuaJScriptValue(func.call(
					((LuaJScriptValue) args[0]).val
			));
			case 2: return new LuaJScriptValue(func.call(
					((LuaJScriptValue) args[0]).val,
					((LuaJScriptValue) args[1]).val
			));
			default: return new LuaJScriptValue(func.call(
					((LuaJScriptValue) args[0]).val,
					((LuaJScriptValue) args[1]).val,
					((LuaJScriptValue) args[2]).val
			));
		}
	}

	public static ScriptValue handle(LibFunction func, ScriptValue[] args) {
		switch (args.length) {
			case 0: return new LuaJScriptValue(func.call());
			case 1: return new LuaJScriptValue(func.call(
					((LuaJScriptValue) args[0]).val
			));
			case 2: return new LuaJScriptValue(func.call(
					((LuaJScriptValue) args[0]).val,
					((LuaJScriptValue) args[1]).val
			));
			case 3: return new LuaJScriptValue(func.call(
					((LuaJScriptValue) args[0]).val,
					((LuaJScriptValue) args[1]).val,
					((LuaJScriptValue) args[2]).val
			));
			default: return new LuaJScriptValue(func.call(
					((LuaJScriptValue) args[0]).val,
					((LuaJScriptValue) args[1]).val,
					((LuaJScriptValue) args[2]).val,
					((LuaJScriptValue) args[3]).val
			));
		}
	}
}
