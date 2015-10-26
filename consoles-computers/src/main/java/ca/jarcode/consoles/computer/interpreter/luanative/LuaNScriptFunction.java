package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptFunction;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;

public class LuaNScriptFunction implements ScriptFunction {

	// this class has it's own members, but they are implemented in C

	// this class has no constructor, but it still needs to be assigned data on creation only visible from C

	public native ScriptValue call(ScriptValue... args);
	public native ScriptValue getAsValue();
}
