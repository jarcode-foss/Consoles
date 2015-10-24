package ca.jarcode.consoles.computer.interpreter.luaj;

import ca.jarcode.consoles.computer.interpreter.Lua;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptFunction;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Array;

public class LuaJScriptValue implements ScriptValue {

	public final LuaValue val;

	public LuaJScriptValue(LuaValue value) {
		val = value;
	}

	@Override
	public Object translateObj() {
		return val.checkuserdata();
	}

	@Override
	public boolean canTranslateObj() {
		return val.isuserdata();
	}

	@Override
	public String translateString() {
		return val.checkjstring();
	}

	@Override
	public boolean canTranslateString() {
		return val.isstring();
	}

	@Override
	public long translateLong() {
		return val.checklong();
	}

	@Override
	public boolean canTranslateLong() {
		return val.isnumber();
	}

	@Override
	public short translateShort() {
		return (short) val.checkint();
	}

	@Override
	public boolean canTranslateShort() {
		return val.isnumber();
	}

	@Override
	public byte translateByte() {
		return (byte) val.checkint();
	}

	@Override
	public boolean canTranslateByte() {
		return val.isnumber();
	}

	@Override
	public int translateInt() {
		return val.checkint();
	}

	@Override
	public boolean canTranslateInt() {
		return val.isnumber();
	}

	@Override
	public float translateFloat() {
		return (float) val.checkdouble();
	}

	@Override
	public boolean canTranslateFloat() {
		return val.isnumber();
	}

	@Override
	public double translateDouble() {
		return val.checkdouble();
	}

	@Override
	public boolean canTranslateDouble() {
		return val.isnumber();
	}

	@Override
	public boolean translateBoolean() {
		return val.checkboolean();
	}

	@Override
	public boolean canTranslateBoolean() {
		return val.isboolean();
	}

	@Override
	public boolean isNull() {
		return val.equals(LuaValue.NIL);
	}

	@Override
	public boolean canTranslateArray() {
		return val.istable();
	}

	@Override
	public Object translateArray(Class type) {
		Class component = type.getComponentType();
		LuaTable table = val.checktable();
		int len = 0;
		for (LuaValue key : table.keys()) {
			if (key.isint() && key.checkint() > len)
				len = key.checkint();
		}
		Object arr = Array.newInstance(component, len);
		for (LuaValue key : table.keys()) {
			if (key.isint())
				Array.set(arr, key.checkint(), Lua.translate(component, new LuaJScriptValue(table.get(key))));
		}
		return arr;
	}

	@Override
	public boolean isFunction() {
		return val.isfunction();
	}

	@Override
	public ScriptFunction getAsFunction() {
		return new LuaJScriptFunction(val.checkfunction());
	}

	@Override
	public void set(ScriptValue key, ScriptValue value) {
		try {
			val.set(((LuaJScriptValue) key).val, ((LuaJScriptValue) key).val);
		}
		catch (LuaError e) {
			throw new LuaJError(e);
		}
	}

	@Override
	public ScriptValue get(ScriptValue key) {
		try {
			return new LuaJScriptValue(val.get(((LuaJScriptValue) key).val));
		}
		catch (LuaError e) {
			throw new LuaJError(e);
		}
	}

	@Override
	public ScriptValue call() {
		try {
			return new LuaJScriptValue(val.call());
		}
		catch (LuaError e) {
			throw new LuaJError(e);
		}
	}

	@Override
	public boolean equals(Object another) {
		return another instanceof LuaJScriptValue && val.equals(((LuaJScriptValue) another).val);
	}
}
