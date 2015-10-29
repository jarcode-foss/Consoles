package ca.jarcode.consoles.computer.interpreter;
import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.Consoles;
import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.interpreter.func.*;
import ca.jarcode.consoles.computer.interpreter.interfaces.FunctionFactory;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptFunction;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;
import ca.jarcode.consoles.computer.interpreter.interfaces.ValueFactory;
import net.jodah.typetools.TypeResolver;
import org.bukkit.Bukkit;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is meant to make method lambdas (using :: operator) usable
 * for function mapping. It looks hacky, but it's incredibly useful.
 *
 * It only supports basic/primitive types, I will add lua tables -> java maps later.
 */
public class Lua {

	public static boolean killAll = false;

	public static Map<String, ComputerLibrary> libraries = new ConcurrentHashMap<>();
	public static Map<String, ScriptFunction> staticFunctions = new ConcurrentHashMap<>();
	public static Map<Thread, FuncPool> pools = new ConcurrentHashMap<>();

	// this helps hugely with making binds for Lua<->Java, but it is definitely the most
	// unique piece of code that I have written.

	public static <R, T1, T2, T3, T4> void map(FourArgFunc<R, T1, T2, T3, T4> func, String luaName) {
		staticFunctions.put(luaName, link(resolveArgTypes(func, FourArgFunc.class, true), func));
	}
	public static <R, T1, T2, T3> void map(ThreeArgFunc<R, T1, T2, T3> func, String luaName) {
		staticFunctions.put(luaName, link(resolveArgTypes(func, ThreeArgFunc.class, true), func));
	}
	public static <R, T1, T2> void map(TwoArgFunc<R, T1, T2> func, String luaName) {
		staticFunctions.put(luaName, link(resolveArgTypes(func, TwoArgFunc.class, true), func));
	}
	public static <R, T1> void map(OneArgFunc<R, T1> func, String luaName) {
		staticFunctions.put(luaName, link(resolveArgTypes(func, OneArgFunc.class, true), func));
	}

	public static <T1, T2, T3, T4> void map(FourArgVoidFunc<T1, T2, T3, T4> func, String luaName) {
		staticFunctions.put(luaName, link(resolveArgTypes(func, FourArgVoidFunc.class, false), func));
	}
	public static <T1, T2, T3> void map(ThreeArgVoidFunc<T1, T2, T3> func, String luaName) {
		staticFunctions.put(luaName, link(resolveArgTypes(func, ThreeArgVoidFunc.class, false), func));
	}
	public static <T1, T2> void map(TwoArgVoidFunc<T1, T2> func, String luaName) {
		staticFunctions.put(luaName, link(resolveArgTypes(func, TwoArgVoidFunc.class, false), func));
	}
	public static <T1> void map(OneArgVoidFunc<T1> func, String luaName) {
		staticFunctions.put(luaName, link(resolveArgTypes(func, OneArgVoidFunc.class, false), func));
	}
	public static <R> void map(NoArgFunc<R> func, String luaName) {
		staticFunctions.put(luaName, link(new Class[0], func));
	}
	public static void map(NoArgVoidFunc func, String luaName) {
		staticFunctions.put(luaName, link(new Class[0], func));
	}


	public static <R, T1, T2, T3, T4> void put(FourArgFunc<R, T1, T2, T3, T4> func, String luaName, FuncPool pool) {
		pool.functions.put(luaName, link(resolveArgTypes(func, FourArgFunc.class, true), func));
	}
	public static <R, T1, T2, T3> void put(ThreeArgFunc<R, T1, T2, T3> func, String luaName, FuncPool pool) {
		pool.functions.put(luaName, link(resolveArgTypes(func, ThreeArgFunc.class, true), func));
	}
	public static <R, T1, T2> void put(TwoArgFunc<R, T1, T2> func, String luaName, FuncPool pool) {
		pool.functions.put(luaName, link(resolveArgTypes(func, TwoArgFunc.class, true), func));
	}
	public static <R, T1> void put(OneArgFunc<R, T1> func, String luaName, FuncPool pool) {
		pool.functions.put(luaName, link(resolveArgTypes(func, OneArgFunc.class, true), func));
	}

	public static <T1, T2, T3, T4> void put(FourArgVoidFunc<T1, T2, T3, T4> func, String luaName, FuncPool pool) {
		pool.functions.put(luaName, link(resolveArgTypes(func, FourArgVoidFunc.class, false), func));
	}
	public static <T1, T2, T3> void put(ThreeArgVoidFunc<T1, T2, T3> func, String luaName, FuncPool pool) {
		pool.functions.put(luaName, link(resolveArgTypes(func, ThreeArgVoidFunc.class, false), func));
	}
	public static <T1, T2> void put(TwoArgVoidFunc<T1, T2> func, String luaName, FuncPool pool) {
		pool.functions.put(luaName, link(resolveArgTypes(func, TwoArgVoidFunc.class, false), func));
	}
	public static <T1> void put(OneArgVoidFunc<T1> func, String luaName, FuncPool pool) {
		pool.functions.put(luaName, link(resolveArgTypes(func, OneArgVoidFunc.class, false), func));
	}
	public static <R> void put(NoArgFunc<R> func, String luaName, FuncPool pool) {
		pool.functions.put(luaName, link(new Class[0], func));
	}
	public static void put(NoArgVoidFunc func, String luaName, FuncPool pool) {
		pool.functions.put(luaName, link(new Class[0], func));
	}

	public static <R, T1, T2, T3, T4> ScriptFunction link(FourArgFunc<R, T1, T2, T3, T4> func) {
		return link(resolveArgTypes(func, FourArgFunc.class, true), func);
	}
	public static <R, T1, T2, T3> ScriptFunction link(ThreeArgFunc<R, T1, T2, T3> func) {
		return link(resolveArgTypes(func, ThreeArgFunc.class, true), func);
	}
	public static <R, T1, T2> ScriptFunction link(TwoArgFunc<R, T1, T2> func) {
		return link(resolveArgTypes(func, TwoArgFunc.class, true), func);
	}
	public static <R, T1> ScriptFunction link(OneArgFunc<R, T1> func) {
		return link(resolveArgTypes(func, OneArgFunc.class, true), func);
	}
	public static <T1, T2, T3, T4> ScriptFunction link(FourArgVoidFunc<T1, T2, T3, T4> func) {
		return link(resolveArgTypes(func, FourArgVoidFunc.class, false), func);
	}
	public static <T1, T2, T3> ScriptFunction link(ThreeArgVoidFunc<T1, T2, T3> func) {
		return link(resolveArgTypes(func, ThreeArgVoidFunc.class, false), func);
	}
	public static <T1, T2> ScriptFunction link(TwoArgVoidFunc<T1, T2> func) {
		return link(resolveArgTypes(func, TwoArgVoidFunc.class, false), func);
	}
	public static <T1> ScriptFunction link(OneArgVoidFunc<T1> func) {
		return link(resolveArgTypes(func, OneArgVoidFunc.class, false), func);
	}
	public static ScriptFunction link(NoArgVoidFunc func) {
		return link(new Class[0], func);
	}
	public static <R> ScriptFunction link(NoArgFunc<R> func) {
		return link(new Class[0], func);
	}
	public static void find(Object inst, FuncPool pool) {
		find(inst.getClass(), inst, pool);
	}
	public static void find(Class type, Object inst, FuncPool pool) {
		List<Method> methodList = new ArrayList<>();
		while (type != Object.class) {
			methodList.addAll(Arrays.asList(type.getDeclaredMethods()));
			type = type.getSuperclass();
		}
		methodList.stream()
				.filter(m -> m.getName().startsWith("lua$"))
				.peek(m -> m.setAccessible(true))
				.map(m -> new AbstractMap.SimpleEntry<>(m.getName().substring(4),
						FunctionFactory.get().createFunction(m, inst)))
				.forEach(entry -> pool.functions.put(entry.getKey(), entry.getValue()));
	}
	public static Object[] toJavaAndRelease(Class[] types, Object... args) {
		for (int t = 0; t < args.length; t++) {
			args[t] = translateAndRelease(types[t], (ScriptValue) args[t]);
		}
		return args;
	}
	// retrieves the computer that the current program is being executed in
	// used in static Java methods that are meant to be visible to lua
	public static Computer context() {
		return findPool().getComputer();
	}

	public static SandboxProgram program() {
		return findPool().getProgram();
	}

	public static boolean terminated() {
		return findPool().getProgram().terminated();
	}

	private static FuncPool findPool() {
		FuncPool pool = pools.get(Thread.currentThread());
		if (pool == null)
			throw new IllegalAccessError("Tried to access lua bindings outside of program thread");
		else return pool;
	}
	public static Class[] resolveArgTypes(Object func, Class<?> type, boolean shift) {
		Class<?>[] arr = TypeResolver.resolveRawArguments(type, func.getClass());
		if (!shift) return arr;
		Class[] ret = new Class[arr.length - 1];
		System.arraycopy(arr, 1, ret, 0, ret.length - 1);
		return ret;
	}
	public static ScriptFunction link(Class[] types, Object func) {
		return FunctionFactory.get().createFunction(types, func);
	}
	public static ScriptValue translateToScriptValue(Object java) {
		ScriptValue globals = findPool().getProgram().globals;
		if (java == null) {
			return ValueFactory.get().nullValue(globals);
		}
		else if (java instanceof ScriptValue) {
			return (ScriptValue) java;
		}
		else if (java instanceof Boolean) {
			return ValueFactory.get().translate((Boolean) java, globals);
		}
		else if (java instanceof Integer) {
			return ValueFactory.get().translate((Integer) java, globals);
		}
		else if (java instanceof Byte) {
			return ValueFactory.get().translate((Byte) java, globals);
		}
		else if (java instanceof Short) {
			return ValueFactory.get().translate((Short) java, globals);
		}
		else if (java instanceof Long) {
			return ValueFactory.get().translate((Long) java, globals);
		}
		else if (java instanceof Double) {
			return ValueFactory.get().translate((Double) java, globals);
		}
		else if (java instanceof Float) {
			return ValueFactory.get().translate((Float) java, globals);
		}
		else if (java instanceof Character) {
			return ValueFactory.get().translate(new String(new char[]{(Character) java}), globals);
		}
		else if (java instanceof String) {
			return ValueFactory.get().translate((String) java, globals);
		}
		// recursive
		else if (java.getClass().isArray()) {
			Object[] arr = new Object[Array.getLength(java)];
			for (int t = 0; t < arr.length; t++)
				arr[t] = Array.get(java, t);
			return ValueFactory.get().list(
					Arrays.asList(arr).stream()
					.map(Lua::translateToScriptValue)
					.toArray(ScriptValue[]::new),
					globals);
		}
		else {
			if (Consoles.debug)
				Computers.getInstance().getLogger().info("[DEBUG] Wrapping java object: " + java.getClass());
			return ValueFactory.get().translateObj(java, globals);
		}
	}

	public static PartialFunctionBind javaCallable(ScriptValue func) {
		if (!func.isFunction()) throw new RuntimeException("expected function");
		return (args) -> {
			ScriptValue[] arr = Arrays.asList(args).stream()
					.map(Lua::translateToScriptValue)
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
		if (type != null && FunctionBind.class.isAssignableFrom(type)
				|| (value.isFunction() && (TypeResolver.Unknown.class == type || type == null))) {
			return javaFunction(value);
		} else if (type != null && ScriptValue.class.isAssignableFrom(type)) {
			return value;
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
		for (Method method : type.getMethods()) {
			if (!Modifier.isStatic(method.getModifiers()) && method.getName().equals(name))
				return method;
		}
		return null;
	}
	// used in native code (don't touch the signature)
	@SuppressWarnings("unused")
	public static long methodId(Method method) {
		int hash = method.hashCode();
		Class<?>[] args = method.getParameterTypes();
		int argmask = 0;
		for (Class<?> arg : args) {
			argmask = 37 * argmask + arg.hashCode();
		}
		return (long) hash << 32 | argmask & 0xFFFFFFFFL;
	}

}
