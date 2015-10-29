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

	// this has been changed in version 1.3 to have a more intuitive
	// way of converting a table to an array

	// it is also much harder to explit. before you could return a table
	// to java like so:

	//   table = {}
	//   table[2^30] = 42
	//   redstone(0, table)

	// even though the type mapping would make it normally throw an error, the following method
	// would still allocate an array size of (2^30), causing a massive memory leak.
	@Override
	public Object translateArray(Class type) {
		Class component = type.getComponentType();
		LuaTable table = val.checktable();
		int i;
		for (i = 0;; i++) {
			if (table.get(i) == LuaValue.NIL) break;
		}
		Object arr = Array.newInstance(component, i);
		for (int t = 0; t < i; t++) {
			Array.set(arr, t, Lua.translateAndRelease(component, new LuaJScriptValue(table.get(t))));
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
	public void release() {
		// do nothing and let Java GC this object
	}

	@Override
	public ScriptValue copy() {
		// we do not need to copy this object, because it is never released
		return this;
	}

	@Override
	public boolean equals(Object another) {
		return another instanceof LuaJScriptValue && val.equals(((LuaJScriptValue) another).val);
	}
}
