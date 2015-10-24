package ca.jarcode.consoles.computer.hooks;

import ca.jarcode.consoles.computer.interpreter.ComputerLibrary;
import ca.jarcode.consoles.computer.interpreter.Lua;
import ca.jarcode.consoles.computer.interpreter.interfaces.FunctionFactory;

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
	 * @see ca.jarcode.consoles.computer.hooks.LibraryCreator#link(java.lang.Class,
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
	private static ComputerLibrary.NamedFunction toLua(Method m, Object inst) {
		ComputerLibrary.NamedFunction function =
				new ComputerLibrary.NamedFunction(FunctionFactory.get().createFunction(m, inst));
		if (m.isAnnotationPresent(LuaName.class)) {
			LuaName annotation = m.getAnnotation(LuaName.class);
			function.setName(annotation.name());
		}
		else function.setName(m.getName());
		return function;
	}
}
