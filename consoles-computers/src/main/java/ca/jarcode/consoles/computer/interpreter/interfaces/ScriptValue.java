package ca.jarcode.consoles.computer.interpreter.interfaces;

public interface ScriptValue {

	Object translateObj();
	boolean canTranslateObj();

	String translateString();
	boolean canTranslateString();

	long translateLong();
	boolean canTranslateLong();

	short translateShort();
	boolean canTranslateShort();

	byte translateByte();
	boolean canTranslateByte();

	int translateInt();
	boolean canTranslateInt();

	float translateFloat();
	boolean canTranslateFloat();

	double translateDouble();
	boolean canTranslateDouble();

	boolean translateBoolean();
	boolean canTranslateBoolean();

	boolean isNull();

	boolean canTranslateArray();
	Object translateArray(Class arrClass);

	boolean isFunction();
	ScriptFunction getAsFunction();

	void set(ScriptValue key, ScriptValue value);

	ScriptValue get(ScriptValue key);

	ScriptValue call();
}
