package ca.jarcode.ascript.interfaces;

import java.io.*;
import java.util.Map;
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
	default ScriptValue load(ScriptValue globals, InputStream is, String path) throws IOException {
		byte[] arr = new byte[is.available()];
		int i;
		for (int t = 0; is.available() > 0; t++) {
			i = is.read();
			if (i == -1) break;
			arr[t] = (byte) i;
		}
		return load(globals, new String(arr), path);
	}
	default ScriptValue load(ScriptValue globals, File file, String path) throws IOException {
		try (InputStream is = new FileInputStream(file)) {
			byte[] arr = new byte[is.available()];
			int i;
			for (int t = 0; is.available() > 0; t++) {
				i = is.read();
				if (i == -1) break;
				arr[t] = (byte) i;
			}
			return load(globals, new String(arr), path);
		}
	}
	ScriptValue load(ScriptValue globals, String raw, String path);
	void setGlobalFunctions(ScriptValue globals, String namespace, Map<String, ScriptValue> mappings);
	void load(ScriptValue globals, FuncPool pool);
	ScriptValue newInstance(FuncPool pool, BooleanSupplier terminated, InputStream stdin, OutputStream stdout, long heap);
	void resetInterrupt(ScriptValue globals);
	void removeRestrictions(ScriptValue globals);
	void close(ScriptValue globals);
	void cleanupThreadContext();
	FunctionUsePolicy functionUsePolicy();
}
