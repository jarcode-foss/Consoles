package ca.jarcode.consoles.computer.interpreter.interfaces;

import ca.jarcode.consoles.computer.interpreter.ComputerLibrary;
import ca.jarcode.consoles.computer.interpreter.FuncPool;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BooleanSupplier;

public interface ScriptEngine {

	ScriptEngine[] factory = new ScriptEngine[1];

	static ScriptEngine get() {
		return factory[0];
	}

	static void assign(ScriptEngine factory) {
		ScriptEngine.factory[0] = factory;
	}

	ScriptValue load(ScriptValue globals, String raw);
	ScriptValue load(ScriptValue globals, ComputerLibrary lib);
	ScriptValue newInstance(FuncPool pool, BooleanSupplier terminated, InputStream stdin, OutputStream stdout, long heap);
	void resetInterrupt(ScriptValue globals);
	void removeRestrictions(ScriptValue globals);
	void close(ScriptValue globals);
}
