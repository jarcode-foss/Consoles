package ca.jarcode.consoles.computer.interpreter.interfaces;

public interface ValueFactory {

	// all values are associated with an program instance (Lua VM)
	// we specify that instance using the globals value

	ValueFactory[] factory = new ValueFactory[1];

	static ValueFactory get() {
		return factory[0];
	}

	static void assign(ValueFactory factory) {
		ValueFactory.factory[0] = factory;
	}

	ScriptValue translate(boolean b, ScriptValue globals);
	ScriptValue translate(float f, ScriptValue globals);
	ScriptValue translate(double d, ScriptValue globals);
	ScriptValue translate(String str, ScriptValue globals);
	ScriptValue translate(int i, ScriptValue globals);
	ScriptValue translate(long l, ScriptValue globals);
	ScriptValue translate(short s, ScriptValue globals);
	ScriptValue translate(byte b, ScriptValue globals);
	ScriptValue translate(char c, ScriptValue globals);

	ScriptValue list(ScriptValue[] values, ScriptValue globals);

	ScriptValue nullValue(ScriptValue globals);

	ScriptValue translateObj(Object obj, ScriptValue globals);
}
