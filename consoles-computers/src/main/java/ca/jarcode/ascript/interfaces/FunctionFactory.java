package ca.jarcode.ascript.interfaces;

import ca.jarcode.ascript.func.NoArgFunc;
import ca.jarcode.ascript.func.NoArgVoidFunc;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public interface FunctionFactory {

	// IMPORTANT NOTE: Functions created in this class are special, they are considered to be
	//                 shared functions (not associated with a VM). This means that they are
	//                 never cleaned up until the thread context is destroyed.

	FunctionFactory[] factory = new FunctionFactory[1];

	static FunctionFactory getDefaultFactory() {
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
