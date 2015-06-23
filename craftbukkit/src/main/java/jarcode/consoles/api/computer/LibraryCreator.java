package jarcode.consoles.api.computer;

import jarcode.consoles.computer.interpreter.ComputerLibrary;
import jarcode.consoles.computer.interpreter.Lua;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class LibraryCreator {

	/**
	 * Links a Java class (and its instance) as a library that is meant to be visible
	 * to Lua programs running in computers. All types are mapped automatically, and
	 * un-convertible types are wrapped into Lua tables (objects).
	 *
	 * @param type the target class to introspect
	 * @param supplier supplier for new instances of the library
	 * @param name the name of the library
	 * @param isRestricted whether the library should operate only when the program is authorized
	 * @param <T> the type of the class to introspect
	 */
	public static <T> void link(Class<T> type, Supplier<? super T> supplier, String name, boolean isRestricted) {
		ComputerLibrary library = new ComputerLibrary(name, isRestricted, () -> methods(type, supplier.get()));
		Lua.libraries.put(name, library);
	}

	/**
	 * Links a Java class as a library.
	 *
	 * @param supplier supplier for new instances of the library
	 * @param name the name of the library
	 * @param isRestricted whether the library should operate only when the program is authorized
	 *
	 * @see jarcode.consoles.api.computer.LibraryCreator#link(java.lang.Class,
	 * java.util.function.Supplier, java.lang.String, boolean)
	 */
	public static void link(Supplier<?> supplier, String name, boolean isRestricted) {
		ComputerLibrary library = new ComputerLibrary(name, isRestricted, () -> {
			Object obj = supplier.get();
			return methods(obj.getClass(), obj);
		});
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
