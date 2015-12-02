package ca.jarcode.ascript;

import ca.jarcode.ascript.interfaces.ScriptLibrary;

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
		ScriptLibrary library = new ScriptLibrary(name, isRestricted, () -> methods(name, type, supplier.get()));
		Script.LIBS.put(name, library);
	}
	// public static ScriptFunction findOrLink(String name, Method method, Object inst, FuncPool pool)

	/**
	 * Links a Java class as a library.
	 *
	 * @param supplier supplier for new instances of the library
	 * @param name the name of the library
	 * @param isRestricted whether the library should operate only when the program is authorized
	 *
	 * @see LibraryCreator#link(java.lang.Class,
	 * java.util.function.Supplier, java.lang.String, boolean)
	 */
	public static void link(Supplier<?> supplier, String name, boolean isRestricted) {
		ScriptLibrary library = new ScriptLibrary(name, isRestricted, () -> {
			Object obj = supplier.get();
			return methods(name, obj.getClass(), obj);
		});
		Script.LIBS.put(name, library);
	}

	private static ScriptLibrary.NamedFunction[] methods(String libraryName, Class<?> type, Object inst) {
		Method[] java = type.getMethods();
		ScriptLibrary.NamedFunction[] lua = new ScriptLibrary.NamedFunction[java.length];
		for (int t = 0; t < java.length; t++)
			lua[t] = toLua(libraryName, java[t], inst);
		return lua;
	}
	private static ScriptLibrary.NamedFunction toLua(String libraryName, Method m, Object inst) {
		String name;
		if (m.isAnnotationPresent(ScriptName.class)) {
			ScriptName annotation = m.getAnnotation(ScriptName.class);
			name = annotation.name();
		}
		else name = m.getName();

		ScriptLibrary.NamedFunction function =
				new ScriptLibrary.NamedFunction((G) -> Script.findOrLink(libraryName + "#" + name, m, inst, G));
		function.setName(name);
		return function;
	}
}
