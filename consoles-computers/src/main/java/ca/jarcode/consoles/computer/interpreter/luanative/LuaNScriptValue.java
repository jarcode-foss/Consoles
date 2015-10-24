package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptFunction;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;

public class LuaNScriptValue implements ScriptValue {

	@Override
	public Object translateObj() {
		return null;
	}

	@Override
	public boolean canTranslateObj() {
		return false;
	}

	@Override
	public String translateString() {
		return null;
	}

	@Override
	public boolean canTranslateString() {
		return false;
	}

	@Override
	public long translateLong() {
		return 0;
	}

	@Override
	public boolean canTranslateLong() {
		return false;
	}

	@Override
	public short translateShort() {
		return 0;
	}

	@Override
	public boolean canTranslateShort() {
		return false;
	}

	@Override
	public byte translateByte() {
		return 0;
	}

	@Override
	public boolean canTranslateByte() {
		return false;
	}

	@Override
	public int translateInt() {
		return 0;
	}

	@Override
	public boolean canTranslateInt() {
		return false;
	}

	@Override
	public float translateFloat() {
		return 0;
	}

	@Override
	public boolean canTranslateFloat() {
		return false;
	}

	@Override
	public double translateDouble() {
		return 0;
	}

	@Override
	public boolean canTranslateDouble() {
		return false;
	}

	@Override
	public boolean translateBoolean() {
		return false;
	}

	@Override
	public boolean canTranslateBoolean() {
		return false;
	}

	@Override
	public boolean isNull() {
		return false;
	}

	@Override
	public boolean canTranslateArray() {
		return false;
	}

	@Override
	public Object translateArray(Class arrClass) {
		return null;
	}

	@Override
	public boolean isFunction() {
		return false;
	}

	@Override
	public ScriptFunction getAsFunction() {
		return null;
	}

	@Override
	public void set(ScriptValue key, ScriptValue value) {

	}

	@Override
	public ScriptValue get(ScriptValue key) {
		return null;
	}

	@Override
	public ScriptValue call() {
		return null;
	}
}
