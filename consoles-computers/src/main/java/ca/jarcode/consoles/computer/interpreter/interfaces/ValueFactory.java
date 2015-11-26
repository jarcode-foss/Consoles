package ca.jarcode.consoles.computer.interpreter.interfaces;

public interface ValueFactory {

	// all values are associated with an program instance (Lua VM)
	// we specify that instance using the globals value

	ValueFactory[] factory = new ValueFactory[1];

	static ValueFactory getDefaultFactory() {
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

	default ScriptValue translate(boolean b, ScriptGlobals globals) {
		return translate(b, globals.value());
	}

	default ScriptValue translate(float f, ScriptGlobals globals) {
		return translate(f, globals.value());
	}

	default ScriptValue translate(double d, ScriptGlobals globals) {
		return translate(d, globals.value());
	}

	default ScriptValue translate(String str, ScriptGlobals globals) {
		return translate(str, globals.value());
	}

	default ScriptValue translate(int i, ScriptGlobals globals) {
		return translate(i, globals.value());
	}

	default ScriptValue translate(long l, ScriptGlobals globals) {
		return translate(l, globals.value());
	}

	default ScriptValue translate(short s, ScriptGlobals globals) {
		return translate(s, globals.value());
	}

	default ScriptValue translate(byte b, ScriptGlobals globals) {
		return translate(b, globals.value());
	}

	default ScriptValue translate(char c, ScriptGlobals globals) {
		return translate(c, globals.value());
	}

	default ScriptValue list(ScriptValue[] values, ScriptGlobals globals) {
		return list(values, globals.value());
	}

	default ScriptValue nullValue(ScriptGlobals globals) {
		return nullValue(globals.value());
	}

	default ScriptValue translateObj(Object obj, ScriptGlobals globals) {
		return translateObj(obj, globals.value());
	}
}
