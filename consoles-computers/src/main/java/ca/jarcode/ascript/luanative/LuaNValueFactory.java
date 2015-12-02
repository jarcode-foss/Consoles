package ca.jarcode.ascript.luanative;

import ca.jarcode.ascript.interfaces.ScriptValue;
import ca.jarcode.ascript.interfaces.ValueFactory;

public class LuaNValueFactory implements ValueFactory {

	// this is a singleton
	
	public native ScriptValue translate(boolean b, ScriptValue globals);
	public native ScriptValue translate(float f, ScriptValue globals);
	public native ScriptValue translate(double d, ScriptValue globals);
	public native ScriptValue translate(String str, ScriptValue globals);
	public native ScriptValue translate(int i, ScriptValue globals);
	public native ScriptValue translate(long l, ScriptValue globals);
	public native ScriptValue translate(short s, ScriptValue globals);
	public native ScriptValue translate(byte b, ScriptValue globals);
	public ScriptValue translate(char c, ScriptValue globals) {
		return translate(new String(new char[] {c}), globals); // there are no characters in Lua
	}
	public native ScriptValue list(ScriptValue[] values, ScriptValue globals);
	public native ScriptValue nullValue(ScriptValue globals);
	public native ScriptValue translateObj(Object obj, ScriptValue globals);
}
