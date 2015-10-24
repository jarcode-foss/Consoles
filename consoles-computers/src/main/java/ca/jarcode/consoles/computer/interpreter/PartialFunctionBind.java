package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;

/*

Special class when mapping functions that can be used as a way to
reference and call a function in Lua as a method parameter.

 */
@FunctionalInterface
public interface PartialFunctionBind {
	ScriptValue call(Object... args);
}
