package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.computer.interpreter.ComputerLibrary;
import ca.jarcode.consoles.computer.interpreter.FuncPool;
import ca.jarcode.consoles.computer.interpreter.interfaces.*;
import jni.LuaEngine;
import org.bukkit.Bukkit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public class LuaNEngine implements ScriptEngine {

	// keep in mind the static init (<sinit>) code cannot reference
	// any classes that require JNI libraries, just in case they
	// aren't loaded yet, or failed to load.

	public static LuaNInterface ENGINE_INTERFACE;

	private static LuaNImpl IMPL = null;

	private static FunctionFactory FUNCTION_FACTORY;
	private static ValueFactory VALUE_FACTORY;
	private static ScriptEngine LUA_NATIVE_ENGINE;

	private static volatile boolean enabled = false;

	public static ScriptGlobals newEnvironment(FuncPool pool, BooleanSupplier terminated,
	                                    InputStream stdin, OutputStream stdout, long heap) {
		return new ScriptGlobals(
				LUA_NATIVE_ENGINE.newInstance(pool, terminated, stdin, stdout, heap),
				LUA_NATIVE_ENGINE, FUNCTION_FACTORY, VALUE_FACTORY
		);
	}

	public static FunctionFactory getFunctionFactory() {
		return FUNCTION_FACTORY;
	}

	public static ValueFactory getValueFactory() {
		return VALUE_FACTORY;
	}

	public static void init() {
		LUA_NATIVE_ENGINE = new LuaNEngine();
		FUNCTION_FACTORY = new LuaNFunctionFactory();
		VALUE_FACTORY = new LuaNValueFactory();
		IMPL = LuaNImpl.JIT;
		enabled = true;
	}

	public static void setImplementation(LuaNImpl implementation) {
		IMPL = implementation;
	}

	public static void install(LuaNImpl impl) {
		if (!enabled) {
			init();
		}
		LuaNEngine.IMPL = impl;
		FunctionFactory.assign(FUNCTION_FACTORY);
		ValueFactory.assign(VALUE_FACTORY);
		ScriptEngine.assign(LUA_NATIVE_ENGINE);
		ENGINE_INTERFACE = new LuaEngine();
		ENGINE_INTERFACE.setdebug(Computers.debug ? 1 : 0);
		ENGINE_INTERFACE.setmaxtime(Computers.maxTimeWithoutInterrupt);
		ENGINE_INTERFACE.setup();
		((LuaNEngine) LUA_NATIVE_ENGINE).L = ENGINE_INTERFACE;
	}

	private List<LuaNInstance> instances = new ArrayList<>();
	private LuaNInterface L;

	private static class LuaNInstance {
		long ptr;
		ScriptValue globals;
		int taskId = -1;
		Runnable threadNameRestore;

		@Override
		public boolean equals(Object another) {
			return (another instanceof LuaNInstance &&
					(((LuaNInstance) another).globals == globals || ((LuaNInstance) another).ptr == ptr));
		}

		LuaNInstance(ScriptValue val) {
			this.globals = val;
		}

		LuaNInstance(long ptr) {
			this.ptr = ptr;
		}

		LuaNInstance() {}
	}

	private LuaNInstance register(long ptr, ScriptValue val, int id) {
		LuaNInstance inst = new LuaNInstance();
		inst.ptr = ptr;
		inst.globals = val;
		inst.taskId = id;
		instances.add(inst);
		return inst;
	}

	private void unregister(ScriptValue val) {
		int idx = instances.indexOf(new LuaNInstance(val));
		LuaNInstance inst = instances.get(idx);
		instances.remove(idx);
		if (inst.taskId != -1)
			Bukkit.getScheduler().cancelTask(inst.taskId);
	}

	private long ptr(ScriptValue val) {
		return inst(val).ptr;
	}

	private LuaNInstance inst(ScriptValue val) {
		return instances.get(instances.indexOf(new LuaNInstance(val)));
	}

	private ScriptValue globals(long ptr) {
		return instances.get(instances.indexOf(new LuaNInstance(ptr))).globals;
	}

	@Override
	public ScriptValue load(ScriptValue globals, String raw, String path) {
		return L.load(ptr(globals), raw, path);
	}

	@Override
	public void load(ScriptValue globals, ComputerLibrary lib) {
		for (ComputerLibrary.NamedFunction function : lib.functions.get()) {
			L.settable(ptr(globals), lib.libraryName, function.getMappedName(), (ScriptValue) function.function);
		}
	}

	@Override
	public ScriptValue newInstance(FuncPool pool, BooleanSupplier terminated, InputStream stdin,
	                               OutputStream stdout, long heap) {
		long ptr; // this is actually a pointer (sue me)
		ptr = L.setupinst(IMPL.val, heap, Computers.interruptCheckInterval);
		ScriptValue globals = L.wrapglobals(ptr);
		if (globals == null) {
			throw new LuaNError("recieved null globals");
		}
		// schedule task on the main thread to constantly check if our program was killed
		// (this could be done in any thread, but it's easiest to use our scheduler)
		LuaNInstance inst;
		if (terminated != null) {
			AtomicBoolean killed = new AtomicBoolean(false);
			int id = Bukkit.getScheduler().scheduleSyncRepeatingTask(Computers.getInstance(), () -> {
				if (terminated.getAsBoolean() && !killed.get()) {
					L.kill(ptr);
					killed.set(true);
				}
			}, 1, 1);
			inst = register(ptr, globals, id);
		}
		else inst = register(ptr, globals, -1);

		if (IMPL != LuaNImpl.JIT_TEST)
			L.blacklist(ptr);

		Thread current =  Thread.currentThread();
		String name = current.getName();
		inst.threadNameRestore = () -> current.setName(name);
		current.setName("LuaN Thread");
		L.pthread_name("LuaN");

		return globals;
	}

	@Override
	public void load(ScriptValue globals, FuncPool pool) {
		// load functions from our pool
		for (Map.Entry<String, ScriptFunction> entry : pool.functions.entrySet()) {
			ScriptValue key = ValueFactory.getDefaultFactory().translate(entry.getKey(), globals);
			globals.set(key, entry.getValue().getAsValue());
			key.release();
		}
	}

	@Override
	public void resetInterrupt(ScriptValue globals) {
		L.interruptreset(ptr(globals));
	}

	@Override
	public void removeRestrictions(ScriptValue globals) {
		L.unrestrict(ptr(globals));
	}

	@Override
	public void close(ScriptValue globals) {
		LuaNInstance inst = inst(globals);
		if (inst.threadNameRestore != null)
			inst.threadNameRestore.run();
		L.pthread_name("java");
		long ptr = ptr(globals);
		unregister(globals);
		L.destroyinst(ptr);
	}
}
