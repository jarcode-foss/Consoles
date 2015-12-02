package ca.jarcode.ascript.luanative;

import ca.jarcode.ascript.interfaces.FunctionFactory;
import ca.jarcode.ascript.interfaces.ScriptFunction;

import java.lang.reflect.Method;

public class LuaNFunctionFactory implements FunctionFactory {

	// this is a singleton

	public native ScriptFunction createFunction(Class[] args, Object func);
	public native ScriptFunction createFunction(Method method, Object inst);
}
