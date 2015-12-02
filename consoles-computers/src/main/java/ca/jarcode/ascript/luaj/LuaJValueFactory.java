package ca.jarcode.ascript.luaj;

import ca.jarcode.ascript.interfaces.ScriptValue;
import ca.jarcode.ascript.interfaces.ValueFactory;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class LuaJValueFactory implements ValueFactory {

	// this implementation ignores the global variable entirely, because
	// values using LuaJ do not need to be associated with an instance.

	// they are also GC'd by the Java VM so memory is not an issue.

	@Override
	public ScriptValue translate(boolean b, ScriptValue globals) {
		return new LuaJScriptValue(LuaValue.valueOf(b));
	}

	@Override
	public ScriptValue translate(float f, ScriptValue globals) {
		return new LuaJScriptValue(LuaValue.valueOf(f));
	}

	@Override
	public ScriptValue translate(double d, ScriptValue globals) {
		return new LuaJScriptValue(LuaValue.valueOf(d));
	}

	@Override
	public ScriptValue translate(String str, ScriptValue globals) {
		return new LuaJScriptValue(LuaValue.valueOf(str));
	}

	@Override
	public ScriptValue translate(int i, ScriptValue globals) {
		return new LuaJScriptValue(LuaValue.valueOf(i));
	}

	@Override
	public ScriptValue translate(long l, ScriptValue globals) {
		return new LuaJScriptValue(LuaValue.valueOf(l));
	}

	@Override
	public ScriptValue translate(short s, ScriptValue globals) {
		return new LuaJScriptValue(LuaValue.valueOf(s));
	}

	@Override
	public ScriptValue translate(byte b, ScriptValue globals) {
		return new LuaJScriptValue(LuaValue.valueOf(b));
	}

	@Override
	public ScriptValue translate(char c, ScriptValue globals) {
		return new LuaJScriptValue(LuaValue.valueOf(c + ""));
	}

	@Override
	public ScriptValue list(ScriptValue[] values, ScriptValue globals) {
		LuaTable table = new LuaTable();
		for (int t = 0; t < values.length; t++)
			table.set(t + 1, ((LuaJScriptValue) values[t]).val);
		return new LuaJScriptValue(table);
	}

	@Override
	public ScriptValue nullValue(ScriptValue globals) {
		return new LuaJScriptValue(LuaValue.NIL);
	}

	@Override
	public ScriptValue translateObj(Object obj, ScriptValue globals) {
		return new LuaJScriptValue(CoerceJavaToLua.coerce(obj));
	}
}
