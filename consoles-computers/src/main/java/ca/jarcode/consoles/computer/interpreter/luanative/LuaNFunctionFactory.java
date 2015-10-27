package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.computer.interpreter.interfaces.FunctionFactory;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptFunction;

import java.lang.reflect.Method;

public class LuaNFunctionFactory implements FunctionFactory {

	// this is a singleton

	public native ScriptFunction createFunction(Class[] args, Object func);
	public native ScriptFunction createFunction(Method method, Object inst);
}
