package jarcode.consoles.computer.interpreter;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.interpreter.func.*;
import net.jodah.typetools.TypeResolver;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is meant to make method lambdas (using :: operator) usable
 * for function mapping. It looks hacky, but it's incredibly useful.
 *
 * It only supports basic/primitive types, I will add lua tables -> java maps later.
 */
public class Lua {

	public static Map<String, LibFunction> staticFunctions = new ConcurrentHashMap<>();
	public static Map<Thread, FuncPool> pools = new ConcurrentHashMap<>();

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

	public static <R, T1, T2, T3, T4> LibFunction link(FourArgFunc<R, T1, T2, T3, T4> func) {
		return link(resolveArgTypes(func, FourArgFunc.class, true), func);
	}
	public static <R, T1, T2, T3> LibFunction link(ThreeArgFunc<R, T1, T2, T3> func) {
		return link(resolveArgTypes(func, ThreeArgFunc.class, true), func);
	}
	public static <R, T1, T2> LibFunction link(TwoArgFunc<R, T1, T2> func) {
		return link(resolveArgTypes(func, TwoArgFunc.class, true), func);
	}
	public static <R, T1> LibFunction link(OneArgFunc<R, T1> func) {
		return link(resolveArgTypes(func, OneArgFunc.class, true), func);
	}
	public static <T1, T2, T3, T4> LibFunction link(FourArgVoidFunc<T1, T2, T3, T4> func) {
		return link(resolveArgTypes(func, FourArgVoidFunc.class, false), func);
	}
	public static <T1, T2, T3> LibFunction link(ThreeArgVoidFunc<T1, T2, T3> func) {
		return link(resolveArgTypes(func, ThreeArgVoidFunc.class, false), func);
	}
	public static <T1, T2> LibFunction link(TwoArgVoidFunc<T1, T2> func) {
		return link(resolveArgTypes(func, TwoArgVoidFunc.class, false), func);
	}
	public static <T1> LibFunction link(OneArgVoidFunc<T1> func) {
		return link(resolveArgTypes(func, OneArgVoidFunc.class, false), func);
	}
	public static LibFunction link(NoArgVoidFunc func) {
		return link(new Class[0], func);
	}
	public static <R> LibFunction link(NoArgFunc<R> func) {
		return link(new Class[0], func);
	}
	public static Computer context() {
		return pools.get(Thread.currentThread()).getComputer();
	}
	public static Class[] resolveArgTypes(Object func, Class<?> type, boolean shift) {
		Class<?>[] arr = TypeResolver.resolveRawArguments(type, func.getClass());
		if (!shift) return arr;
		Class[] ret = new Class[arr.length - 1];
		System.arraycopy(arr, 1, ret, 0, ret.length - 1);
		return ret;
	}
	public static LibFunction link(Class[] types, Object func) {

		if (types.length == 1)
			return new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue v1) {
					return translateLua(Lua.call(func, types, v1));
				}
			};
		else if (types.length == 2)
			return new TwoArgFunction() {

				@Override
				public LuaValue call(LuaValue v1, LuaValue v2) {
					return translateLua(Lua.call(func, types, v1, v2));
				}
			};
		else if (types.length == 3)
			return new ThreeArgFunction() {

				@Override
				public LuaValue call(LuaValue v1, LuaValue v2, LuaValue v3) {
					return translateLua(Lua.call(func, types, v1, v2, v3));
				}
			};
		else if (types.length == 0)
			return new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					return translateLua(Lua.call(func, types));
				}
			};

		return new LibFunction() {
			@Override
			public LuaValue call() {
				return translateLua(Lua.call(func, types));
			}

			@Override
			public LuaValue call(LuaValue v1) {
				return translateLua(Lua.call(func, types, v1));
			}

			@Override
			public LuaValue call(LuaValue v1, LuaValue v2) {
				return translateLua(Lua.call(func, types, v1, v2));
			}

			@Override
			public LuaValue call(LuaValue v1, LuaValue v2, LuaValue v3) {
				return translateLua(Lua.call(func, types, v1, v2, v3));
			}

			@Override
			public LuaValue call(LuaValue v1, LuaValue v2, LuaValue v3, LuaValue v4) {
				return translateLua(Lua.call(func, types, v1, v2, v3, v4));
			}

			@Override
			public LuaValue invoke(Varargs value) {
				Object[] total = new Object[types.length];
				for (int t = 0; t < types.length; t++) {
					total[t] = translate(types[t], value.arg(t), t);
				}
				return translateLua(Lua.call(func, types, total));
			}
		};
	}
	private static LuaValue translateLua(Object java) {
		if (java == null) {
			return LuaValue.valueOf(true);
		}
		else if (java instanceof Boolean) {
			return LuaValue.valueOf((Boolean) java);
		}
		else if (java instanceof Integer) {
			return LuaValue.valueOf((Integer) java);
		}
		else if (java instanceof Byte) {
			return LuaValue.valueOf((Byte) java);
		}
		else if (java instanceof Short) {
			return LuaValue.valueOf((Short) java);
		}
		else if (java instanceof Long) {
			return LuaValue.valueOf((Long) java);
		}
		else if (java instanceof Double) {
			return LuaValue.valueOf((Double) java);
		}
		else if (java instanceof Float) {
			return LuaValue.valueOf((Float) java);
		}
		else if (java instanceof Character) {
			return LuaValue.valueOf(new String(new char[]{(Character) java}));
		}
		else if (java instanceof String) {
			return LuaValue.valueOf((String) java);
		}
		else throw new RuntimeException("Unsupported argument: " + java.getClass() + ", value: " + java);
	}
	private static Object translate(Class<?> type, LuaValue value, int i) {
		if (type == boolean.class || type == Boolean.class) {
			return value.checkboolean(i);
		}
		else if (type == int.class || type == Integer.class) {
			return value.checkint(i);
		}
		else if (type == byte.class || type == Byte.class) {
			return (byte) value.checkint(i);
		}
		else if (type == short.class || type == Short.class) {
			return (short) value.checkint(i);
		}
		else if (type == long.class || type == Long.class) {
			return value.checklong(i);
		}
		else if (type == double.class || type == Double.class) {
			return value.checkdouble(i);
		}
		else if (type == float.class || type == Float.class) {
			return (float) value.checknumber(i).checkdouble();
		}
		else if (type == char.class || type == Character.class) {
			return value.checkjstring().charAt(0);
		}
		else if (type == String.class) {
			return value.checkjstring();
		}
		else throw new RuntimeException("Unsupported argument: " + type);
	}
	@SuppressWarnings("unchecked")
	private static Object call(Object func, Class[] types, Object... args) {
		for (int t = 0; t < args.length; t++) {
			args[t] = translate(types[t], (LuaValue) args[t], t + 1);
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
}
