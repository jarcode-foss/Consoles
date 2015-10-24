package ca.jarcode.consoles.computer.interpreter.luaj;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;
import ca.jarcode.consoles.computer.interpreter.interfaces.ValueFactory;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class LuaJValueFactory implements ValueFactory {
	@Override
	public ScriptValue translate(boolean b) {
		return new LuaJScriptValue(LuaValue.valueOf(b));
	}

	@Override
	public ScriptValue translate(float f) {
		return new LuaJScriptValue(LuaValue.valueOf(f));
	}

	@Override
	public ScriptValue translate(double d) {
		return new LuaJScriptValue(LuaValue.valueOf(d));
	}

	@Override
	public ScriptValue translate(String str) {
		return new LuaJScriptValue(LuaValue.valueOf(str));
	}

	@Override
	public ScriptValue translate(int i) {
		return new LuaJScriptValue(LuaValue.valueOf(i));
	}

	@Override
	public ScriptValue translate(long l) {
		return new LuaJScriptValue(LuaValue.valueOf(l));
	}

	@Override
	public ScriptValue translate(short s) {
		return new LuaJScriptValue(LuaValue.valueOf(s));
	}

	@Override
	public ScriptValue translate(byte b) {
		return new LuaJScriptValue(LuaValue.valueOf(b));
	}

	@Override
	public ScriptValue translate(char c) {
		return new LuaJScriptValue(LuaValue.valueOf(c + ""));
	}

	@Override
	public ScriptValue list(ScriptValue[] values) {
		LuaTable table = new LuaTable();
		for (int t = 0; t < values.length; t++)
			table.set(t, ((LuaJScriptValue) values[t]).val);
		return new LuaJScriptValue(table);
	}

	@Override
	public ScriptValue nullValue() {
		return new LuaJScriptValue(LuaValue.NIL);
	}

	@Override
	public ScriptValue translateObj(Object obj) {
		return new LuaJScriptValue(CoerceJavaToLua.coerce(obj));
	}
}
