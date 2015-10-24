package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.computer.interpreter.ComputerLibrary;
import ca.jarcode.consoles.computer.interpreter.FuncPool;
import ca.jarcode.consoles.computer.interpreter.interfaces.FunctionFactory;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptEngine;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;
import ca.jarcode.consoles.computer.interpreter.interfaces.ValueFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BooleanSupplier;

public class LuaNEngine implements ScriptEngine {

	private static LuaNImpl IMPL = null;

	public static void install(LuaNImpl impl) {
		LuaNEngine.IMPL = impl;
		FunctionFactory.assign(new LuaNFunctionFactory());
		ValueFactory.assign(new LuaNValueFactory());
		ScriptEngine.assign(new LuaNEngine());
	}

	@Override
	public ScriptValue load(ScriptValue globals, String raw) {
		return null;
	}

	@Override
	public ScriptValue load(ScriptValue globals, ComputerLibrary lib) {
		return null;
	}

	@Override
	public ScriptValue newInstance(FuncPool pool, BooleanSupplier terminated, InputStream stdin,
	                               OutputStream stdout, long heap) {
		// long ptr = setupinst(IMPL.val, heap);
		return null;
	}

	@Override
	public void resetInterrupt(ScriptValue globals) {

	}

	@Override
	public void removeRestrictions(ScriptValue globals) {

	}

	@Override
	public void close(ScriptValue globals) {

	}

	// private native long setupinst(int impl, long heap);
	// private native long unrestrict(long ptr);
}
