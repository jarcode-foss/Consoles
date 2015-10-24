package ca.jarcode.consoles.computer.interpreter.interfaces;

public interface ValueFactory {

	ValueFactory[] factory = new ValueFactory[1];

	static ValueFactory get() {
		return factory[0];
	}

	static void assign(ValueFactory factory) {
		ValueFactory.factory[0] = factory;
	}

	ScriptValue translate(boolean b);
	ScriptValue translate(float f);
	ScriptValue translate(double d);
	ScriptValue translate(String str);
	ScriptValue translate(int i);
	ScriptValue translate(long l);
	ScriptValue translate(short s);
	ScriptValue translate(byte b);
	ScriptValue translate(char c);

	ScriptValue list(ScriptValue[] values);

	ScriptValue nullValue();

	ScriptValue translateObj(Object obj);
}
