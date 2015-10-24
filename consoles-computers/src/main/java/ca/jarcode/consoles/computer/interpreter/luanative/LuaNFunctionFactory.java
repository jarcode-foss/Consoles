package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.computer.interpreter.interfaces.FunctionFactory;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptFunction;

import java.lang.reflect.Method;

public class LuaNFunctionFactory implements FunctionFactory {
	@Override
	public ScriptFunction createFunction(Class[] args, Object func) {
		return null;
	}

	@Override
	public ScriptFunction createFunction(Method method, Object inst) {
		return null;
	}
}
