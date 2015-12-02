package ca.jarcode.ascript.interfaces;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/*

This is a wrapper class for the globals variable. For the script interface, globals are just a
special value that can be indexed and set - this class wraps that, and provides utility methods
from the engine implementation.

 */
public class ScriptGlobals {

	private final ScriptValue value;
	private final ScriptEngine engine;
	private final FunctionFactory functionFactory;
	private final ValueFactory valueFactory;

	public ScriptGlobals(ScriptValue value, ScriptEngine engine,
	                     FunctionFactory functionFactory, ValueFactory valueFactory) {
		this.value = value;
		this.engine = engine;
		this.functionFactory = functionFactory;
		this.valueFactory = valueFactory;
	}

	public ScriptValue value() {
		return value;
	}

	public boolean isNull() {
		return value == null || value.isNull();
	}

	public void set(ScriptValue key, ScriptValue value) {
		this.value.set(key, value);
	}

	public FunctionFactory getFunctionFactory() {
		return functionFactory;
	}

	public ValueFactory getValueFactory() {
		return valueFactory;
	}

	public ScriptEngine getEngine() {
		return engine;
	}

	public ScriptValue get(ScriptValue key) {
		return value.get(key);
	}

	public void release() {
		value.release();
	}

	public ScriptValue load(String raw) {
		return engine.load(value, raw, "?");
	}

	public ScriptValue load(InputStream stream, String path) throws IOException {
		return engine.load(value, stream, path);
	}

	public ScriptValue load(File file, String path) throws IOException {
		return engine.load(value, file, path);
	}

	public ScriptValue load(String raw, String path) {
		return engine.load(value, raw, path);
	}

	public void load(FuncPool pool) {
		engine.load(value, pool);
	}

	public void load(ScriptLibrary lib) {
		Map<String, ScriptValue> map = new HashMap<>();
		for (ScriptLibrary.NamedFunction func : lib.functions.get())
			map.put(func.getMappedName(), func.preparedFunction.apply(this).getAsValue());
		engine.setGlobalFunctions(value, lib.libraryName, map);
	}

	public void resetInterrupt() {
		engine.resetInterrupt(value);
	}

	public void removeRestrictions() {
		engine.removeRestrictions(value);
	}

	public void close() {
		engine.close(value);
	}

	public void cleanupThreadContext() {
		engine.cleanupThreadContext();
	}
}
