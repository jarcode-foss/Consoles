package ca.jarcode.ascript.interfaces;

/**
 * A generic script value. It is assumed this type is immutable, unless it represents the set
 * of globals for the underlying scripting language and interpreter.
 */
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

	/**
	 * <p>Frees resources from the value. After releasing the value, it is considered to be 'dead',
	 * and should not be used afterwards. Releasing the value should not affect any underlying
	 * or counterpart lua values that this object copies or wraps, it should only prevent
	 * this object from being further used.</p>
	 *
	 * <p>Implementations of this method may be left blank if this value has nothing to free.</p>
	 *
	 * <p>After use of a value in a function, library, or type, the value should be released.</p>
	 */
	void release();

	/**
	 * Copies the value, such that if the original value is released, this copy of the value will remain.
	 *
	 * @return a copy of this value
	 */
	ScriptValue copy();
}
