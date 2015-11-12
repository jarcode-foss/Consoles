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

	public static LuaNInterface NATIVE_ENGINE;

	private static LuaNImpl IMPL = null;

	public static void install(LuaNImpl impl) {
		LuaNEngine.IMPL = impl;
		FunctionFactory.assign(new LuaNFunctionFactory());
		ValueFactory.assign(new LuaNValueFactory());
		LuaNEngine luaN = new LuaNEngine();
		ScriptEngine.assign(luaN);
		NATIVE_ENGINE = new LuaEngine();
		NATIVE_ENGINE.setdebug(Computers.debug ? 1 : 0);
		NATIVE_ENGINE.setmaxtime(Computers.maxTimeWithoutInterrupt);
		NATIVE_ENGINE.setup();
		luaN.L = NATIVE_ENGINE;
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
			return (another instanceof LuaNInstance && ((LuaNInstance) another).globals == globals);
		}

		LuaNInstance(ScriptValue val) {
			this.globals = val;
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

	@Override
	public ScriptValue load(ScriptValue globals, String raw) {
		return L.load(ptr(globals), raw);
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
		Thread current =  Thread.currentThread();
		String name = current.getName();
		inst.threadNameRestore = () -> current.setName(name);
		current.setName("LuaN Thread");
		L.pthread_name("LuaN");

		// load functions from our pool
		for (Map.Entry<String, ScriptFunction> entry : pool.functions.entrySet()) {
			ScriptValue key = ValueFactory.get().translate(entry.getKey(), globals);
			globals.set(key, entry.getValue().getAsValue());
			key.release();
		}

		return globals;
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
