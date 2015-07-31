package ca.jarcode.consoles.computer.interpreter;

import org.luaj.vm2.LuaValue;

/*

Special class when mapping functions that can be used as a way to
reference and call a function in Lua as a method parameter.

 */
@FunctionalInterface
public interface PartialFunctionBind {
	LuaValue call(Object... args);
}
