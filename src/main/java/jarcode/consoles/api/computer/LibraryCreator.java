package jarcode.consoles.api.computer;

import jarcode.consoles.computer.interpreter.ComputerLibrary;
import jarcode.consoles.computer.interpreter.Lua;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LibraryCreator {

	/**
	 * Links a Java object as a library visible to lua code. The immediate type
	 * is the class that is introspected when mapping out methods.
	 *
	 * @param instance the target object to use
	 * @param name the same of the library
	 * @see {@link jarcode.consoles.api.computer.LibraryCreator#link(java.lang.Class type,
	 * java.lang.Object instance, java.lang.String name)}
	 */
	public static void link(Object instance, String name, boolean isRestricted) {
		link(instance.getClass(), instance, name, isRestricted);
	}
	/**
	 * Links a Java class (and its instance) as a library that is meant to be visible
	 * to Lua programs running in computers. All types are mapped automatically, and
	 * un-convertible types are wrapped into Lua tables (objects).
	 *
	 * @param type the target class to introspect
	 * @param instance the instance of the class to use
	 * @param name the name of the library
	 */
	public static void link(Class<?> type, Object instance, String name, boolean isRestricted) {
		ComputerLibrary library = new ComputerLibrary(name, methods(type, instance), isRestricted);
		Lua.libraries.put(name, library);
	}
	private static ComputerLibrary.NamedFunction[] methods(Class<?> type, Object inst) {
		Method[] java = type.getMethods();
		ComputerLibrary.NamedFunction[] lua = new ComputerLibrary.NamedFunction[java.length];
		for (int t = 0; t < java.length; t++)
			lua[t] = toLua(java[t], inst);
		return lua;
	}
	private static ComputerLibrary.NamedFunction toLua(Method m, final Object inst) {
		Class[] types = m.getParameterTypes();
		ComputerLibrary.NamedFunction function = new ComputerLibrary.NamedFunction() {
			@Override
			public LuaValue call() {
				try {
					return Lua.translateLua(m.invoke(inst));
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new LuaError(e);
				}
			}

			@Override
			public LuaValue call(LuaValue v) {
				try {
					return Lua.translateLua(m.invoke(inst, Lua.toJava(types, v)));
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new LuaError(e);
				}
			}

			@Override
			public LuaValue call(LuaValue v, LuaValue v1) {
				try {
					return Lua.translateLua(m.invoke(inst, Lua.toJava(types, v, v1)));
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new LuaError(e);
				}
			}

			@Override
			public LuaValue call(LuaValue v, LuaValue v1, LuaValue v2) {
				try {
					return Lua.translateLua(m.invoke(inst, Lua.toJava(types, v, v1, v2)));
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new LuaError(e);
				}
			}

			@Override
			public LuaValue call(LuaValue v, LuaValue v1, LuaValue v2, LuaValue v3) {
				try {
					return Lua.translateLua(m.invoke(inst, Lua.toJava(types, v, v1, v2, v3)));
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new LuaError(e);
				}
			}
		};
		if (m.isAnnotationPresent(LuaName.class)) {
			LuaName annotation = m.getAnnotation(LuaName.class);
			function.setName(annotation.name());
		}
		else function.setName(m.getName());
		return function;
	}
}
