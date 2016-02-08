package ca.jarcode.ascript;
import ca.jarcode.ascript.interfaces.*;
import ca.jarcode.consoles.Computers;
import ca.jarcode.ascript.interfaces.ScriptLibrary;
import ca.jarcode.consoles.computer.interpreter.FunctionBind;
import ca.jarcode.consoles.computer.interpreter.PartialFunctionBind;
import ca.jarcode.ascript.func.*;
import ca.jarcode.ascript.util.ThreadMap;
import net.jodah.typetools.TypeResolver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * This is meant to make method lambdas (using :: operator) usable
 * for function mapping. It looks hacky, but it's incredibly useful.
 */
public class Script {

	public static boolean killAll = false;

	public static Map<String, ScriptLibrary> LIBS = new ConcurrentHashMap<>();
	public static Map<String, Function<FuncPool, ScriptFunction>> STATIC_FUNCS = new ConcurrentHashMap<>();
	public static Map<Thread, FuncPool> POOLS = new ConcurrentHashMap<>();

	// these are mappings to functions for engines that can re-use ScriptFunctions
	public static Map<ScriptEngine, Map<String, ScriptFunction>> FUNCS = new ConcurrentHashMap<>();
	public static ThreadMap<Map<ScriptEngine, Map<String, ScriptFunction>>> CONTEXT_FUNCS = new ThreadMap<>();

	// this helps hugely with making binds for Lua<->Java, but it is definitely the most
	// unique piece of code that I have written.

	// Create function that will be mapped to pools when FuncPool.mapStaticFunctions() is called

	public static <R, T1, T2, T3, T4> void map(FourArgFunc<R, T1, T2, T3, T4> func, String name) {
		STATIC_FUNCS.put(name, pool ->
				findOrLink(name, resolveArgTypes(func, FourArgFunc.class, true), func, pool));
	}
	public static <R, T1, T2, T3> void map(ThreeArgFunc<R, T1, T2, T3> func, String name) {
		STATIC_FUNCS.put(name, pool ->
				findOrLink(name, resolveArgTypes(func, ThreeArgFunc.class, true), func, pool));
	}
	public static <R, T1, T2> void map(TwoArgFunc<R, T1, T2> func, String name) {
		STATIC_FUNCS.put(name, pool ->
				findOrLink(name, resolveArgTypes(func, TwoArgFunc.class, true), func, pool));
	}
	public static <R, T1> void map(OneArgFunc<R, T1> func, String name) {
		STATIC_FUNCS.put(name, pool ->
				findOrLink(name, resolveArgTypes(func, OneArgFunc.class, true), func, pool));
	}

	public static <T1, T2, T3, T4> void map(FourArgVoidFunc<T1, T2, T3, T4> func, String name) {
		STATIC_FUNCS.put(name, pool ->
				findOrLink(name, resolveArgTypes(func, FourArgVoidFunc.class, false), func, pool));
	}
	public static <T1, T2, T3> void map(ThreeArgVoidFunc<T1, T2, T3> func, String name) {
		STATIC_FUNCS.put(name, pool ->
				findOrLink(name, resolveArgTypes(func, ThreeArgVoidFunc.class, false), func, pool));
	}
	public static <T1, T2> void map(TwoArgVoidFunc<T1, T2> func, String name) {
		STATIC_FUNCS.put(name, pool ->
				findOrLink(name, resolveArgTypes(func, TwoArgVoidFunc.class, false), func, pool));
	}
	public static <T1> void map(OneArgVoidFunc<T1> func, String name) {
		STATIC_FUNCS.put(name, pool ->
				findOrLink(name, resolveArgTypes(func, OneArgVoidFunc.class, false), func, pool));
	}
	public static <R> void map(NoArgFunc<R> func, String name) {
		STATIC_FUNCS.put(name, pool ->
				findOrLink(name, new Class[0], func, pool));
	}
	public static void map(NoArgVoidFunc func, String name) {
		STATIC_FUNCS.put(name, pool ->
				findOrLink(name, new Class[0], func, pool));
	}

	// Create function and put it into a function pool, unsafe because it is sensitive to the thread context

	public static <R, T1, T2, T3, T4> void unsafeMap(FourArgFunc<R, T1, T2, T3, T4> func, String name, FuncPool<?> pool) {
		pool.functions.put(name,
				findOrLink(name, resolveArgTypes(func, FourArgFunc.class, true), func, pool));
	}
	public static <R, T1, T2, T3> void unsafeMap(ThreeArgFunc<R, T1, T2, T3> func, String name, FuncPool<?> pool) {
		pool.functions.put(name,
				findOrLink(name, resolveArgTypes(func, ThreeArgFunc.class, true), func, pool));
	}
	public static <R, T1, T2> void unsafeMap(TwoArgFunc<R, T1, T2> func, String name, FuncPool<?> pool) {
		pool.functions.put(name,
				findOrLink(name, resolveArgTypes(func, TwoArgFunc.class, true), func, pool));
	}
	public static <R, T1> void unsafeMap(OneArgFunc<R, T1> func, String name, FuncPool<?> pool) {
		pool.functions.put(name,
				findOrLink(name, resolveArgTypes(func, OneArgFunc.class, true), func, pool));
	}

	public static <T1, T2, T3, T4> void unsafeMap(FourArgVoidFunc<T1, T2, T3, T4> func, String name, FuncPool<?> pool) {
		pool.functions.put(name,
				findOrLink(name, resolveArgTypes(func, FourArgVoidFunc.class, false), func, pool));
	}
	public static <T1, T2, T3> void unsafeMap(ThreeArgVoidFunc<T1, T2, T3> func, String name, FuncPool<?> pool) {
		pool.functions.put(name,
				findOrLink(name, resolveArgTypes(func, ThreeArgVoidFunc.class, false), func, pool));
	}
	public static <T1, T2> void unsafeMap(TwoArgVoidFunc<T1, T2> func, String name, FuncPool<?> pool) {
		pool.functions.put(name,
				findOrLink(name, resolveArgTypes(func, TwoArgVoidFunc.class, false), func, pool));
	}
	public static <T1> void unsafeMap(OneArgVoidFunc<T1> func, String name, FuncPool<?> pool) {
		pool.functions.put(name,
				findOrLink(name, resolveArgTypes(func, OneArgVoidFunc.class, false), func, pool));
	}
	public static <R> void unsafeMap(NoArgFunc<R> func, String name, FuncPool<?> pool) {
		pool.functions.put(name,
				findOrLink(name, new Class[0], func, pool));
	}
	public static void unsafeMap(NoArgVoidFunc func, String name, FuncPool<?> pool) {
		pool.functions.put(name,
				findOrLink(name, new Class[0], func, pool));
	}

	public static void find(Object inst, FuncPool pool) {
		find(inst.getClass(), inst, pool, "$");
	}
	public static void find(Class<?> type, Object inst, FuncPool pool) {
		find(type, inst, pool, "$");
	}
	public static void find(Class<?> type, Object inst, FuncPool<?> pool, String prefix) {
		if (inst instanceof Class || inst == null || !type.isAssignableFrom(inst.getClass()))
			throw new IllegalArgumentException("'inst' must be an instance of 'type'");
		while (type != Object.class) {
			Arrays.asList(type.getDeclaredMethods()).stream()
					.filter(m -> !Modifier.isStatic(m.getModifiers()))
					.filter(m -> prefix == null || prefix.isEmpty() || m.getName().startsWith(prefix))
					.peek(m -> m.setAccessible(true))
					.map(m -> new AbstractMap.SimpleEntry<>(m.getName().substring(prefix.length()),
							findOrLink(m.getName(), m, inst, pool)))
					.forEach(entry -> pool.functions.put(entry.getKey(), entry.getValue()));
			type = type.getSuperclass();
		}
	}
	public static Object[] toJavaAndRelease(Class[] types, Object... args) {
		for (int t = 0; t < args.length; t++) {
			args[t] = translateAndRelease(types[t], (ScriptValue) args[t]);
		}
		return args;
	}
	public static FuncPool<?> contextPool() {
		FuncPool<?> pool = POOLS.get(Thread.currentThread());
		if (pool == null)
			throw new IllegalAccessError("Tried to access lua bindings outside of program thread");
		else return pool;
	}
	public static boolean terminatedInContext() {
		return contextPool().terminatedSupplier().getAsBoolean();
	}
	public static Class[] resolveArgTypes(Object func, Class<?> type, boolean shift) {
		Class<?>[] arr = TypeResolver.resolveRawArguments(type, func.getClass());
		if (!shift) return arr;
		Class[] ret = new Class[arr.length - 1];
		System.arraycopy(arr, 1, ret, 0, ret.length - 1);
		return ret;
	}
	@SuppressWarnings("unused")
	@Deprecated
	public static ScriptFunction unsafeLink(Class[] types, Object func, FuncPool pool) {
		return pool.getGlobals().getFunctionFactory().createFunction(types, func);
	}
	private static void registerContextFunc(ScriptEngine engine, String name, ScriptFunction f) {
		Map<ScriptEngine, Map<String, ScriptFunction>> map = CONTEXT_FUNCS.get();
		if (map == null) {
			map = new ConcurrentHashMap<>();
			CONTEXT_FUNCS.put(map);
		}
		Map<String, ScriptFunction> funcMap = map.get(engine);
		if (funcMap == null) {
			funcMap = new ConcurrentHashMap<>();
			map.put(engine, funcMap);
		}
		funcMap.put(name, f);
	}
	private static void registerFunc(ScriptEngine engine, String name, ScriptFunction f) {
		Map<String, ScriptFunction> map = FUNCS.get(engine);
		if (map == null) {
			map = new ConcurrentHashMap<>();
			FUNCS.put(engine, map);
		}
		map.put(name, f);
	}
	private static ScriptFunction findContextFunc(ScriptEngine engine, String name) {
		Map<ScriptEngine, Map<String, ScriptFunction>> map = CONTEXT_FUNCS.get();
		if (map == null) {
			map = new ConcurrentHashMap<>();
			CONTEXT_FUNCS.put(map);
			CONTEXT_FUNCS.purge();
			return null;
		}
		Map<String, ScriptFunction> funcMap = map.get(engine);
		CONTEXT_FUNCS.purge();
		return funcMap == null ? null : funcMap.get(name);
	}
	private static ScriptFunction findFunc(ScriptEngine engine, String name) {
		Map<String, ScriptFunction> map = FUNCS.get(engine);
		return map == null ? null : map.get(name);
	}
	public static ScriptFunction findOrLink(String name, Method method, Object inst, FuncPool pool) {
		return findOrLink(name, method, inst, pool.getGlobals());
	}
	public static ScriptFunction findOrLink(String name, Method method, Object inst, ScriptGlobals G) {
		ScriptFunction f;
		switch (G.getEngine().functionUsePolicy()) {
			case DO_NOT_RECYCLE:
				return G.getFunctionFactory().createFunction(method, inst);
			case RECYCLE:
				f = findFunc(G.getEngine(), name);
				if (f == null) {
					f = G.getFunctionFactory().createFunction(method, inst);
					registerFunc(G.getEngine(), name, f);
				}
				return f;
			case RECYCLE_IN_CONTEXT:
				f = findContextFunc(G.getEngine(), name);
				if (f == null) {
					f = G.getFunctionFactory().createFunction(method, inst);
					registerContextFunc(G.getEngine(), name, f);
				}
				return f;
			default:
				throw new IllegalArgumentException();
		}
	}
	public static ScriptFunction findOrLink(String name, Class[] types, Object func, FuncPool pool) {
		return findOrLink(name, types, func, pool.getGlobals());
	}
	public static ScriptFunction findOrLink(String name, Class[] types, Object func, ScriptGlobals G) {
		ScriptFunction f;
		switch (G.getEngine().functionUsePolicy()) {
			case DO_NOT_RECYCLE:
				return G.getFunctionFactory().createFunction(types, func);
			case RECYCLE:
				f = findFunc(G.getEngine(), name);
				if (f == null) {
					f = G.getFunctionFactory().createFunction(types, func);
					registerFunc(G.getEngine(), name, f);
				}
				return f;
			case RECYCLE_IN_CONTEXT:
				f = findContextFunc(G.getEngine(), name);
				if (f == null) {
					f = G.getFunctionFactory().createFunction(types, func);
					registerContextFunc(G.getEngine(), name, f);
				}
				return f;
			default:
				throw new IllegalArgumentException();
		}
	}
	public static ScriptValue[] translateToScriptValues(ScriptGlobals G, Object... java) {
		ScriptValue[] arr = new ScriptValue[java.length];
		for (int t = 0; t < arr.length; t++)
			arr[t] = translateToScriptValue(java[t], G);
		return arr;
	}
	public static ScriptValue translateToScriptValue(Object java) {
		return translateToScriptValue(java, contextPool().getGlobals());
	}
	public static ScriptValue translateToScriptValue(Object java, ScriptGlobals G) {
		ScriptValue globals = G.value();
		if (java == null) {
			return G.getValueFactory().nullValue(globals);
		}
		else if (java instanceof ScriptValue) {
			return (ScriptValue) java;
		}
		else if (java instanceof Boolean) {
			return G.getValueFactory().translate((Boolean) java, globals);
		}
		else if (java instanceof Integer) {
			return G.getValueFactory().translate((Integer) java, globals);
		}
		else if (java instanceof Byte) {
			return G.getValueFactory().translate((Byte) java, globals);
		}
		else if (java instanceof Short) {
			return G.getValueFactory().translate((Short) java, globals);
		}
		else if (java instanceof Long) {
			return G.getValueFactory().translate((Long) java, globals);
		}
		else if (java instanceof Double) {
			return G.getValueFactory().translate((Double) java, globals);
		}
		else if (java instanceof Float) {
			return G.getValueFactory().translate((Float) java, globals);
		}
		else if (java instanceof Character) {
			return G.getValueFactory().translate(new String(new char[]{(Character) java}), globals);
		}
		else if (java instanceof String) {
			return G.getValueFactory().translate((String) java, globals);
		}
		// recursive
		else if (java.getClass().isArray()) {
			Object[] arr = new Object[Array.getLength(java)];
			for (int t = 0; t < arr.length; t++)
				arr[t] = Array.get(java, t);
			return G.getValueFactory().list(
					Arrays.asList(arr).stream()
							.map(Script::translateToScriptValue)
							.toArray(ScriptValue[]::new),
					globals);
		}
		else {
			if (Computers.debug) {
				if (Computers.getInstance() != null)
					Computers.getInstance().getLogger().info("[DEBUG] Wrapping java object: " + java.getClass());
				else
					System.out.println("J: Wrapping java object: " + java.getClass());
			}
			return G.getValueFactory().translateObj(java, globals);
		}
	}

	public static PartialFunctionBind javaCallable(ScriptValue func) {
		if (!func.isFunction()) throw new RuntimeException("expected function");
		return (args) -> {
			ScriptValue[] arr = Arrays.asList(args).stream()
					.map(Script::translateToScriptValue)
					.toArray(ScriptValue[]::new);
			switch (arr.length) {
				case 0: return func.getAsFunction().call();
				case 1: return func.getAsFunction().call(arr[0]);
				case 2: return func.getAsFunction().call(arr[0], arr[1]);
				case 3: return func.getAsFunction().call(arr[0], arr[1], arr[2]);
			}
			throw new RuntimeException("function has too many arguments");
		};
	}

	public static FunctionBind javaFunction(ScriptValue value) {
		return (args) -> coerce(javaCallable(value).call(args));
	}

	private static Object coerce(ScriptValue value) {
		if (value.canTranslateBoolean())
			return value.translateBoolean();
		else if (value.canTranslateDouble())
			return value.translateDouble();
		else if (value.canTranslateString())
			return value.translateString();
		else if (value.isFunction())
			return javaFunction(value);
		else if (value.isNull())
			return null;
		else if (value.canTranslateArray())
			return value.translateArray(Object[].class);
		else if (value.canTranslateObj())
			return value.translateObj();
		else throw new RuntimeException("could not assume type for: " + value.toString() + " ("
					+ value.getClass().getSimpleName() + ")");
	}
	public static Object translate(Class<?> type, ScriptValue value) {
		if (type == Void.class) {
			return null;
		}
		if (type != null && ScriptValue.class.isAssignableFrom(type)) {
			return value;
		} else if (type != null && PartialFunctionBind.class.isAssignableFrom(type)) {
			return javaCallable(value);
		} else if (type != null && FunctionBind.class.isAssignableFrom(type)
				|| (value.isFunction() && (TypeResolver.Unknown.class == type || type == null))) {
			return javaFunction(value);
		}
		// some of these are unsupported on non Oracle/Open JSE VMs
		else if (type == Runnable.class) {
			if (!value.isFunction()) throw new RuntimeException("expected function");
			return (Runnable) value.getAsFunction()::call;
		} else if (type == boolean.class || type == Boolean.class
				|| value.canTranslateBoolean()) {
			return value.translateBoolean();
		} else if (type == int.class || type == Integer.class
				|| (value.canTranslateInt() && (TypeResolver.Unknown.class == type || type == null))) {
			return value.translateInt();
		} else if (type == byte.class || type == Byte.class) {
			return value.translateByte();
		} else if (type == short.class || type == Short.class) {
			return value.translateShort();
		} else if (type == long.class || type == Long.class) {
			return value.translateLong();
		} else if (type == double.class || type == Double.class) {
			return value.translateDouble();
		} else if (type == float.class || type == Float.class) {
			return value.translateFloat();
		} else if (type == char.class || type == Character.class) {
			return value.translateString().charAt(0);
		} else if (type == String.class || value.canTranslateString()) {
			return value.translateString();
		} else if (value.isNull()) {
			return null;
		} else if (value.canTranslateObj()) {
			return value.translateObj();
		} else if (type != null && type.isArray() && value.canTranslateArray()) {
			return value.translateArray(type);
		} else if (value.canTranslateArray() && type == Object.class) {
			return value.translateArray(Object[].class);
		} else throw new RuntimeException("Unsupported argument: " + type
				+ ", lua: " + value.getClass().getSimpleName() + ", data: " + value.toString());
	}
	public static Object translateAndRelease(Class<?> type, ScriptValue value) {
		try {
			return translate(type, value);
		}
		finally {
			value.release();
		}
	}
	@SuppressWarnings("unchecked")
	public static Object callAndRelease(Object func, Class[] types, Object... args) {
		for (int t = 0; t < args.length; t++) {
			args[t] = translateAndRelease(types[t], (ScriptValue) args[t]);
		}
		if (func instanceof OneArgFunc) {
			return ((OneArgFunc) func).call(args[0]);
		}
		else if (func instanceof TwoArgFunc) {
			return ((TwoArgFunc) func).call(args[0], args[1]);
		}
		else if (func instanceof ThreeArgFunc) {
			return ((ThreeArgFunc) func).call(args[0], args[1], args[2]);
		}
		else if (func instanceof FourArgFunc) {
			return ((FourArgFunc) func).call(args[0], args[1], args[2], args[3]);
		}
		else if (func instanceof NoArgFunc) {
			return ((NoArgFunc) func).call();
		}
		else if (func instanceof NoArgVoidFunc) {
			((NoArgVoidFunc) func).call();
			return null;
		}
		else if (func instanceof OneArgVoidFunc) {
			((OneArgVoidFunc) func).call(args[0]);
			return null;
		}
		else if (func instanceof TwoArgVoidFunc) {
			((TwoArgVoidFunc) func).call(args[0], args[1]);
			return null;
		}
		else if (func instanceof ThreeArgVoidFunc) {
			((ThreeArgVoidFunc) func).call(args[0], args[1], args[2]);
			return null;
		}
		else if (func instanceof FourArgVoidFunc) {
			((FourArgVoidFunc) func).call(args[0], args[1], args[2], args[3]);
			return null;
		}
		else throw new RuntimeException("Unsupported interface");
	}
	public static void main(Runnable task) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Computers.getInstance(), task);
	}
	// used in native code (don't touch the signature)
	@SuppressWarnings("unused")
	public static Method resolveMethod(Object object, String name) {
		Class<?> type = object.getClass();
		while (type != Object.class) {
			for (Method method : type.getMethods()) {
				if (!Modifier.isStatic(method.getModifiers()) && method.getName().equals(name)) {
					return method;
				}
			}
			type = type.getSuperclass();
		}
		return null;
	}
	// used in native code (don't touch the signature)
	@SuppressWarnings("unused")
	public static long methodId(Method method) {
		//TODO: remove exception gaurd
		try {
			int hash = method.hashCode();
			Class<?>[] args = method.getParameterTypes();
			int argmask = 0;
			for (Class<?> arg : args) {
				argmask = 37 * argmask + arg.hashCode();
			}
			return (long) hash << 32 | argmask & 0xFFFFFFFFL;
		}
		catch (NullPointerException e) {
			System.out.println(method.getName() + ", " + method.getDeclaringClass());
			throw e;
		}
	}

	// This is called by engines to further handle exceptions after
	// the engine has already passed the exception to lua as an error.
	//
	// LuaJ does not use this method, it instead passes the exception as
	// a cause under a LuaJError
	@SuppressWarnings("unused")
	public static void handleJavaException(Throwable ex) {
		if (Computers.debug) {
			if (Computers.getInstance() != null) {
				Computers.getInstance().getLogger().warning(
						"[DEBUG] exception thrown in Java method during execution of Lua program:"
				);
				Computers.getInstance().getLogger().warning("\n" + ExceptionUtils.getFullStackTrace(ex));
			}
			else {
				System.out.println("J: exception thrown in Java method during execution of Lua program:");
				System.out.println("\n" + ExceptionUtils.getFullStackTrace(ex));
			}
		}
	}

	public static void cleanupThreadContext() {
		ScriptEngine.getDefaultEngine().cleanupThreadContext();
	}
}
