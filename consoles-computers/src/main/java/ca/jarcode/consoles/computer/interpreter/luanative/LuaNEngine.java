package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.Consoles;
import ca.jarcode.consoles.computer.interpreter.ComputerLibrary;
import ca.jarcode.consoles.computer.interpreter.FuncPool;
import ca.jarcode.consoles.computer.interpreter.interfaces.FunctionFactory;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptEngine;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;
import ca.jarcode.consoles.computer.interpreter.interfaces.ValueFactory;
import jni.LuaEngine;
import org.bukkit.Bukkit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public class LuaNEngine implements ScriptEngine {

	public static LuaNInterface NATIVE_ENGINE;

	private static LuaNImpl IMPL = null;

	public static void install(LuaNImpl impl) {
		LuaNEngine.IMPL = impl;
		FunctionFactory.assign(new LuaNFunctionFactory());
		ValueFactory.assign(new LuaNValueFactory());
		ScriptEngine.assign(new LuaNEngine());
		NATIVE_ENGINE = new LuaEngine();
		NATIVE_ENGINE.setdebug(Computers.debug ? 1 : 0);
		NATIVE_ENGINE.setmaxtime(Computers.maxTimeWithoutInterrupt);
	}

	private List<Long> ptrs = new ArrayList<>();
	private List<ScriptValue> values = new ArrayList<>();
	private List<Integer> ids = new ArrayList<>();
	private final LuaNInterface L = NATIVE_ENGINE;

	private void register(long ptr, ScriptValue val, int id) {
		ptrs.add(ptr);
		values.add(val);
		ids.add(id);
	}

	private void unregister(ScriptValue val) {
		int idx = values.indexOf(val);
		int id = ids.get(idx);
		values.remove(idx);
		ptrs.remove(idx);
		ids.remove(idx);
		Bukkit.getScheduler().cancelTask(id);
	}

	private long ptr(ScriptValue val) {
		return ptrs.get(values.indexOf(val));
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
		// schedule task on the main thread to constantly check if our program was killed
		// (this could be done in any thread, but it's easiest to use our scheduler)
		AtomicBoolean killed = new AtomicBoolean(false);
		int id = Bukkit.getScheduler().scheduleSyncRepeatingTask(Computers.getInstance(), () -> {
			if (terminated.getAsBoolean() && !killed.get()) {
				L.kill(ptr);
				killed.set(true);
			}
		}, 1, 1);
		register(ptr, globals, id);
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
		long ptr = ptr(globals);
		unregister(globals);
		L.destroyinst(ptr);
	}
}
