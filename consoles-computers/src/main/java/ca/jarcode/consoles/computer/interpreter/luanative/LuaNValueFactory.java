package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;
import ca.jarcode.consoles.computer.interpreter.interfaces.ValueFactory;

public class LuaNValueFactory implements ValueFactory {

	// this is a singleton
	
	public native ScriptValue translate(boolean b);
	public native ScriptValue translate(float f);
	public native ScriptValue translate(double d);
	public native ScriptValue translate(String str);
	public native ScriptValue translate(int i);
	public native ScriptValue translate(long l);
	public native ScriptValue translate(short s);
	public native ScriptValue translate(byte b);
	public native ScriptValue translate(char c);
	public native ScriptValue list(ScriptValue[] values);
	public native ScriptValue nullValue();
	public native ScriptValue translateObj(Object obj);
}
