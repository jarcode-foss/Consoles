package ca.jarcode.consoles.computer.interpreter.luaj;

import ca.jarcode.consoles.computer.interpreter.ComputerLibrary;
import ca.jarcode.consoles.computer.interpreter.FuncPool;
import ca.jarcode.consoles.computer.interpreter.SandboxProgram;
import ca.jarcode.consoles.computer.interpreter.interfaces.*;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
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
	public void load(ScriptValue globals, ComputerLibrary lib) {
		((LuaJScriptValue) globals).val.load(buildLibrary(lib));
	}

	@Override
	public void load(ScriptValue globals, FuncPool pool) {
		// load functions from our pool
		for (Map.Entry<String, ScriptFunction> entry : pool.functions.entrySet()) {
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
			globals.STDOUT = SandboxProgram.dummyPrintStream();
		else
			try {
				globals.STDOUT = new PrintStream(out, true, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// should never happen unless the JVM somehow doesn't support UTF-8 encoding (wat)
				throw new RuntimeException(e);
			}

		// we handle errors with exceptions, so this will always be a dummy writer.
		globals.STDERR = SandboxProgram.dummyPrintStream();

		// set stdin
		if (in == null)
			globals.STDIN = SandboxProgram.dummyInputStream();
		else
			globals.STDIN = in;

		return new LuaJScriptValue(globals);
	}

	private TwoArgFunction buildLibrary(ComputerLibrary library) {
		return new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored, LuaValue global) {
				LuaTable table = new LuaTable(0, 30);
				global.set(library.libraryName, table);
				for (ComputerLibrary.NamedFunction function : library.functions.get()) {
					table.set(function.getMappedName(), ((LuaJScriptFunction) function.function).func);
				}
				global.get("package").get("loaded").set(library.libraryName, table);
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
	public boolean functionsAreReusable() {
		return true;
	}

	@Override
	public int hashCode() {
		return 1;
	}
}
