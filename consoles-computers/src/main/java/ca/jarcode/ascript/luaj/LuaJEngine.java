package ca.jarcode.ascript.luaj;

import ca.jarcode.ascript.Joint;
import ca.jarcode.ascript.interfaces.*;
import ca.jarcode.ascript.interfaces.ScriptLibrary;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;

import java.io.*;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class LuaJEngine implements ScriptEngine {

	private static FunctionFactory FUNCTION_FACTORY;
	private static ValueFactory VALUE_FACTORY;

	private static ScriptEngine LUA_JAVA_ENGINE;

	private static volatile boolean enabled = false;

	public static void init() {
		FUNCTION_FACTORY = new LuaJFunctionFactory();

		VALUE_FACTORY = new LuaJValueFactory();

		LUA_JAVA_ENGINE = new LuaJEngine();
		enabled = true;
	}

	public static ScriptGlobals newEnvironment(FuncPool pool, BooleanSupplier terminated,
	                                           InputStream stdin, OutputStream stdout, long heap) {
		if (!enabled) {
			init();
		}
		return new ScriptGlobals(
				LUA_JAVA_ENGINE.newInstance(pool, terminated, stdin, stdout, heap),
				LUA_JAVA_ENGINE, FUNCTION_FACTORY, VALUE_FACTORY
		);
	}

	public static FunctionFactory getFunctionFactory() {
		return FUNCTION_FACTORY;
	}

	public static ValueFactory getValueFactory() {
		return VALUE_FACTORY;
	}

	public static void install() {
		if (!enabled) {
			init();
		}
		FunctionFactory.assign(new LuaJFunctionFactory());
		ValueFactory.assign(new LuaJValueFactory());
		ScriptEngine.assign(new LuaJEngine());
	}

	@Override
	public ScriptValue load(ScriptValue globals, String raw, String path) {
		return new LuaJScriptValue(((Globals) ((LuaJScriptValue) globals).val).load(raw, path));
	}

	@Override
	public void setGlobalFunctions(ScriptValue globals, String namespace, Map<String, ScriptValue> mappings) {
		((LuaJScriptValue) globals).val.load(buildLibrary(namespace, mappings));
	}

	@Override
	public void load(ScriptValue globals, FuncPool pool) {
		// load functions from our pool
		for (Map.Entry<String, ScriptFunction> entry : ((FuncPool<?>) pool).functions.entrySet()) {
			((LuaJScriptValue) globals).val.set(entry.getKey(), ((LuaJScriptFunction) entry.getValue()).func);
		}
	}

	@Override
	public ScriptValue newInstance(FuncPool pool, BooleanSupplier terminated, InputStream in,
	                               OutputStream out, long heap) {

		// create our globals for Lua. We use a special kind of globals
		// that allows us to finalize variables.
		LuaJEmbeddedGlobals globals = new LuaJEmbeddedGlobals(terminated);

		// Load libraries from LuaJ. I left a bunch of libraries from the
		// JSE standards to have less possibilities for users to exploit
		// them.
		globals.load(new JseBaseLib());
		globals.load(new PackageLib());
		globals.load(new Bit32Lib());
		globals.load(new TableLib());
		globals.load(new StringLib());
		globals.load(new BaseLib());

		// I added a missing function to the math library
		globals.load(new LuaJEmbeddedMathLib());

		// Load our debugging library, which is used to terminate the program
		globals.load(globals.interruptLib);


		// install
		LoadState.install(globals);
		LuaC.install(globals);

		// Block some functions
		globals.set("load", LuaValue.NIL);
		globals.set("loadfile", LuaValue.NIL);
		// require should be used instead
		globals.set("dofile", LuaValue.NIL);

		globals.set("__impl", LuaValue.valueOf("LuaJ"));

		// set stdout
		if (out == null)
			globals.STDOUT = dummyPrintStream();
		else
			try {
				globals.STDOUT = new PrintStream(out, true, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// should never happen unless the JVM somehow doesn't support UTF-8 encoding (wat)
				throw new RuntimeException(e);
			}

		// we handle errors with exceptions, so this will always be a dummy writer.
		globals.STDERR = dummyPrintStream();

		// set stdin
		if (in == null)
			globals.STDIN = dummyInputStream();
		else
			globals.STDIN = in;

		return new LuaJScriptValue(globals);
	}


	// returns a dummy input stream
	public static InputStream dummyInputStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				return 0;
			}
		};
	}

	// returns a dummy print stream
	public static PrintStream dummyPrintStream() {
		return new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {}
		}) {
			@Override
			public void println(String x) {}

			@Override
			public void println(Object x) {}
		};
	}

	private TwoArgFunction buildLibrary(String namespace, Map<String, ScriptValue> mappings) {
		return new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored, LuaValue global) {
				LuaTable table = new LuaTable(0, 30);
				global.set(namespace, table);
				for (Map.Entry<String, ScriptValue> entry : mappings.entrySet()) {
					table.set(entry.getKey(), ((LuaJScriptFunction) entry.getValue().getAsFunction()).func);
				}
				global.get("package").get("loaded").set(namespace, table);
				return table;
			}
		};
	}

	@Override
	public void resetInterrupt(ScriptValue globals) {
		((LuaJEmbeddedGlobals) ((LuaJScriptValue) globals).val).interruptLib.update();
	}

	@Override
	public void removeRestrictions(ScriptValue globals) {
		LuaJEmbeddedGlobals g = ((LuaJEmbeddedGlobals) ((LuaJScriptValue) globals).val);
		if (!g.restricted) {
			g.load(new CoroutineLib());
			g.load(new OsLib());
			g.restricted = true;
		}
	}

	@Override
	public void close(ScriptValue globals) {
		// do nothing! This is a pure-java implementation, so just leave it to GC.
	}

	@Override
	public void cleanupThreadContext() {
		// do nothing again
	}

	@Override
	public FunctionUsePolicy functionUsePolicy() {
		return FunctionUsePolicy.RECYCLE;
	}

	@Override
	public int hashCode() {
		return 1;
	}
}
