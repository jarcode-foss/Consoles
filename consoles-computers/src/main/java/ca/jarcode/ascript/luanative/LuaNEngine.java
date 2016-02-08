package ca.jarcode.ascript.luanative;

import ca.jarcode.ascript.Joint;
import ca.jarcode.ascript.interfaces.*;
import ca.jarcode.consoles.Computers;
import jni.LuaEngine;
import org.bukkit.Bukkit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
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

	private static List<Thread> contextDestroyedThreads = new ArrayList<>();

	public static ScriptGlobals newEnvironment(FuncPool pool, BooleanSupplier terminated,
	                                    InputStream stdin, OutputStream stdout, long heap) {
		if (!enabled) {
			init();
		}
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
		ENGINE_INTERFACE.setdebug(Joint.DEBUG_MODE ? 1 : 0);
		ENGINE_INTERFACE.setmaxtime(Joint.MAX_TIME_WITHOUT_INTERRUPT);
		ENGINE_INTERFACE.setup();
		((LuaNEngine) LUA_NATIVE_ENGINE).L = ENGINE_INTERFACE;
	}

	private final Object INSTANCE_LOCK = new Object();
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
		synchronized (INSTANCE_LOCK) {
			instances.add(inst);
		}
		return inst;
	}

	private void unregister(ScriptValue val) {
		synchronized (INSTANCE_LOCK) {
			int idx = instances.indexOf(new LuaNInstance(val));
			LuaNInstance inst = instances.get(idx);
			instances.remove(idx);
			if (inst.taskId != -1)
				Bukkit.getScheduler().cancelTask(inst.taskId);
		}
	}

	private long ptr(ScriptValue val) {
		return inst(val).ptr;
	}

	private LuaNInstance inst(ScriptValue val) {
		synchronized (INSTANCE_LOCK) {
			return instances.get(instances.indexOf(new LuaNInstance(val)));
		}
	}

	private ScriptValue globals(long ptr) {
		synchronized (INSTANCE_LOCK) {
			return instances.get(instances.indexOf(new LuaNInstance(ptr))).globals;
		}
	}

	@Override
	public ScriptValue load(ScriptValue globals, String raw, String path) {
		return L.load(ptr(globals), raw, path);
	}

	@Override
	public void setGlobalFunctions(ScriptValue globals, String namespace, Map<String, ScriptValue> mappings) {
		for (Map.Entry<String, ScriptValue> entry : mappings.entrySet()) {
			L.settable(ptr(globals), namespace, entry.getKey(), entry.getValue());
		}
	}

	@Override
	public ScriptValue newInstance(FuncPool pool, BooleanSupplier terminated, InputStream stdin,
	                               OutputStream stdout, long heap) {
		
		if (LuaNScriptValue.TRACK_INSTANCES && LuaNScriptValue.TRACKED.get() == null) {
			LuaNScriptValue.TRACKED.set(new HashMap<>());
		}
		
		long ptr; // this is actually a pointer (sue me)
		ptr = L.setupinst(IMPL.val, heap, Joint.INTERRUPT_CHECK_INTERVAL);
		
		ScriptValue globals = L.wrapglobals(ptr);
		if (globals == null || globals.isNull()) {
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
		for (Map.Entry<String, ScriptFunction> entry : ((FuncPool<?>) pool).functions.entrySet()) {
			ScriptValue key = VALUE_FACTORY.translate(entry.getKey(), globals);
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
		if (globals == null) throw new IllegalArgumentException();
		LuaNInstance inst = inst(globals);
		
		if (LuaNScriptValue.TRACK_INSTANCES) {
			int count = LuaNScriptValue.releaseRemainingContextValues(inst.ptr);
			if (count > 0) {
				System.out.printf("Collected %d hanging script values - possible memory leak!\n", count);
			}
		}
		
		if (inst.threadNameRestore != null)
			inst.threadNameRestore.run();
		L.pthread_name("java");
		long ptr = ptr(globals);
		unregister(globals);
		L.destroyinst(ptr);
	}

	// Cleanup thread-specific data, called when a thread ends that has been used for LuaN
	
	@Override
	public synchronized void cleanupThreadContext() {
		Thread current = Thread.currentThread();
		if (contextDestroyedThreads.contains(current))
			throw new IllegalStateException("context already destroyed for this thread");
		else {
			L.thread_end(); /* native hook */
			
			if (LuaNScriptValue.TRACK_INSTANCES) {
				LuaNScriptValue.TRACKED.destroyContext();
			}
			contextDestroyedThreads.add(current);
			Iterator<Thread> it = contextDestroyedThreads.iterator();
			while (it.hasNext()) {
				if (!it.next().isAlive())
					it.remove();
			}
		}
	}

	// LuaN's script values are only visible from the thread that created them, so we
	// cannot share them across different threads, however, it does have a concept of
	// 'shared' values, which can be used independent of an engine instance.

	@Override
	public FunctionUsePolicy functionUsePolicy() {
		return FunctionUsePolicy.RECYCLE_IN_CONTEXT;
	}

	@Override
	public int hashCode() {
		return 0;
	}
}
