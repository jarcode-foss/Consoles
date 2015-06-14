package jarcode.consoles.computer.manual;

import jarcode.consoles.computer.boot.Kernel;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.interpreter.FunctionBind;
import org.bukkit.ChatColor;
import org.luaj.vm2.LuaValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ManualManager {

	public static final Map<FSProvidedProgram, ManualEntry> PROVIDED_MAP;

	private static Map<String, ManualEntry> MAP = new ConcurrentHashMap<>();

	static {

		HashMap<FSProvidedProgram, ManualEntry> providedMap = new HashMap<>();

		// load manuals for all the provided (java) programs
		for (FSProvidedProgram provided : Kernel.programs()) {
			Class type = provided.getClass();
			Annotation[] arr = type.getAnnotations();
			ProvidedManual man = (ProvidedManual) Arrays.asList(arr).stream()
					.filter(a -> ProvidedManual.class.isAssignableFrom(a.annotationType()))
					.findFirst()
					.orElseGet(() -> null);
			if (man != null) {
				providedMap.put(provided, new ManualEntry((name) ->
						"Manual for program: " + ChatColor.GREEN + name,
						man.author(), man.contents(), man.version(), null, null));
			}
		}
		PROVIDED_MAP = Collections.unmodifiableMap(providedMap);
	}

	public static void load(Class type) {
		map(type, name -> name.startsWith("lua$") || name.startsWith("lua_") ?
				name.substring(4) : null);
	}
	public static void loadType(Class type) {
		map(type, name -> type.getSimpleName() + ":" + name);
	}
	public static Map<String, ManualEntry> manuals() {
		return Collections.unmodifiableMap(MAP);
	}
	public static void map(Class type, Function<String, String> finder) {
		for (Method method : type.getDeclaredMethods()) {
			Annotation[] arr = method.getAnnotations();

			String name = finder.apply(method.getName());

			if (name == null)
				continue;

			FunctionManual man = (FunctionManual) Arrays.asList(arr).stream()
					.filter(a -> FunctionManual.class.isAssignableFrom(a.annotationType()))
					.findFirst()
					.orElseGet(() -> null);

			// builder for method synopsis
			StringBuilder b = new StringBuilder();
			b.append(name);

			b.append('(');
			Class<?>[] params = method.getParameterTypes();
			Annotation[][] ann = method.getParameterAnnotations();

			String[] argInfo = new String[params.length];

			for (int t = 0; t < params.length; t++) {

				String typeName = typeName(params[t]);

				b.append(typeName);

				Arg argAnn = (Arg) Arrays.asList(ann[t]).stream()
						.filter(a -> Arg.class.isAssignableFrom(a.annotationType()))
						.findFirst()
						.orElseGet(() -> null);

				b.append(' ');

				if (argAnn != null) {
					b.append(argAnn.name());
					argInfo[t] = ChatColor.YELLOW + argAnn.name() + ChatColor.WHITE + " - " + argAnn.info();
				}
				else {
					b.append("arg");
					b.append(t);
					argInfo[t] = ChatColor.YELLOW + "arg" + t + ChatColor.WHITE + " - missing info";
				}

				if (t != params.length - 1)
					b.append(", ");
			}

			b.append(")");

			// function argument string builder
			StringBuilder ab = new StringBuilder();

			for (String str : argInfo) {
				ab.append("\t\t");
				ab.append(str);
				ab.append('\n');
			}
			MAP.put(name, new ManualEntry((n) ->
					"Manual for function: " + ChatColor.GREEN + n,
					null, man == null ? "missing manual" : man.value(), null, b.toString(), ab.toString()));
		}
	}
	private static String typeName(Class type) {
		if (type == String.class)
			return "string";
		else if (type == Integer.class)
			return "int";
		else if (type == Double.class)
			return "double";
		else if (type == Long.class)
			return "long";
		else if (type == Short.class)
			return "short";
		else if (type == Byte.class)
			return "byte";
		else if (type == Character.class)
			return "char";
		else if (type == FunctionBind.class)
			return "function";
		else if (LuaValue.class.isAssignableFrom(type))
			return "var";
		else return type.getSimpleName();
	}
}
