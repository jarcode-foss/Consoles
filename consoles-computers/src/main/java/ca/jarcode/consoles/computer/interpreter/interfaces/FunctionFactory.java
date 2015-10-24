package ca.jarcode.consoles.computer.interpreter.interfaces;

import ca.jarcode.consoles.computer.interpreter.func.NoArgFunc;
import ca.jarcode.consoles.computer.interpreter.func.NoArgVoidFunc;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public interface FunctionFactory {

	FunctionFactory[] factory = new FunctionFactory[1];

	static FunctionFactory get() {
		return factory[0];
	}

	static void assign(FunctionFactory factory) {
		FunctionFactory.factory[0] = factory;
	}

	/**
	 * creates a function in the underling engine
	 *
	 * @param args type args
	 * @param func something from the func package
	 * @return the function
	 */
	ScriptFunction createFunction(Class[] args, Object func);
	// same thing
	ScriptFunction createFunction(Method method, Object inst);

	default ScriptFunction createFunction(Runnable runnable) {
		return createFunction(new Class[0], (NoArgVoidFunc) runnable::run);
	}

	default ScriptFunction createFunction(Supplier<ScriptValue> supplier) {
		return createFunction(new Class[0], (NoArgFunc) supplier::get);
	}
}
