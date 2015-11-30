package ca.jarcode.consoles.computer.interpreter.interfaces;

import ca.jarcode.consoles.computer.interpreter.ComputerLibrary;
import ca.jarcode.consoles.computer.interpreter.FuncPool;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BooleanSupplier;

public interface ScriptEngine {

	ScriptEngine[] factory = new ScriptEngine[1];

	// The 'preferred' way to create a new script environment.
	static ScriptGlobals newEnvironment(FuncPool pool, BooleanSupplier terminated,
	                                    InputStream stdin, OutputStream stdout, long heap) {
		return new ScriptGlobals(
				getDefaultEngine().newInstance(pool, terminated, stdin, stdout, heap),
				getDefaultEngine(), FunctionFactory.getDefaultFactory(), ValueFactory.getDefaultFactory()
		);
	}

	static ScriptEngine getDefaultEngine() {
		return factory[0];
	}

	static void assign(ScriptEngine factory) {
		ScriptEngine.factory[0] = factory;
	}

	ScriptValue load(ScriptValue globals, String raw, String path);
	void load(ScriptValue globals, ComputerLibrary lib);
	void load(ScriptValue globals, FuncPool pool);
	ScriptValue newInstance(FuncPool pool, BooleanSupplier terminated, InputStream stdin, OutputStream stdout, long heap);
	void resetInterrupt(ScriptValue globals);
	void removeRestrictions(ScriptValue globals);
	void close(ScriptValue globals);
	void cleanupThreadContext();
	boolean functionsAreReusable();
}
