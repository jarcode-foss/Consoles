package jarcode.consoles.computer.manual;

import jarcode.consoles.computer.boot.Kernel;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.computer.interpreter.FunctionBind;
import jarcode.consoles.computer.interpreter.Lua;
import org.bukkit.ChatColor;
import org.luaj.vm2.LuaValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ManualManager {

	public static final Map<FSProvidedProgram, ManualEntry> PROVIDED_MANUALS;
	public static final Map<String, ManualEntry> MANUALS = new ConcurrentHashMap<>();
	public static final Map<String, ManualEntry> TYPE_MANUALS = new ConcurrentHashMap<>();

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
		PROVIDED_MANUALS = Collections.unmodifiableMap(providedMap);

		Lua.map(ManualManager::lua_manual_functionNames, "manual_functionNames");
	}

	public static void load(Class type) {
		map(type, name -> name.startsWith("lua$") || name.startsWith("lua_") ?
				name.substring(4) : null);
	}
	public static void loadType(Class type) {
		map(type, name -> name.contains("$") ? null : type.getSimpleName() + ":" + name, false);
	}
	public static Map<String, ManualEntry> manuals() {
		Map<String, ManualEntry> map = new HashMap<>();
		map.putAll(MANUALS);
		map.putAll(TYPE_MANUALS);
		return Collections.unmodifiableMap(map);
	}
	public static void map(Class type, Function<String, String> finder) {
		map(type, finder, true);
	}
	public static void map(Class type, Function<String, String> finder, boolean searchPrivate) {
		for (Method method : searchPrivate ? type.getDeclaredMethods() : type.getMethods()) {

			if (method.getDeclaringClass() != type)
				continue;

			Annotation[] arr = method.getAnnotations();

			String name = finder.apply(method.getName());

			if (name == null)
				continue;

			FunctionManual man = (FunctionManual) Arrays.asList(arr).stream()
					.filter(a -> FunctionManual.class.isAssignableFrom(a.annotationType()))
					.findFirst()
					.orElseGet(() -> null);

			if (man == null)
				continue;

			// builder for method synopsis
			StringBuilder b = new StringBuilder();
			b.append(ChatColor.AQUA);
			b.append(name.replace(":", ChatColor.WHITE + ":" + ChatColor.AQUA));
			b.append(ChatColor.WHITE);

			b.append('(');
			Class<?>[] params = method.getParameterTypes();
			Annotation[][] ann = method.getParameterAnnotations();

			String[] argInfo = new String[params.length];

			for (int t = 0; t < params.length; t++) {

				String typeName = typeName(params[t]);

				b.append(ChatColor.GRAY);
				b.append(typeName);
				b.append(ChatColor.WHITE);

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
			if (argInfo.length > 0)
				ab.append('\n');
			ab.append("\t\treturns ");
			ab.append(ChatColor.YELLOW);
			if (method.getReturnType() != null
					&& method.getReturnType() != Void.class
					&& method.getReturnType() != void.class)
				ab.append(typeName(method.getReturnType()));
			else ab.append("nil");
			MANUALS.put(name, new ManualEntry((n) ->
					"Manual for function: " + ChatColor.GREEN + n,
					null, ChatColor.translateAlternateColorCodes('&', man.value()), null, b.toString(), ab.toString()));
		}
	}
	public static String[] lua_manual_functionNames() {
		return MANUALS.keySet().stream().toArray(String[]::new);
	}
	private static String typeName(Class type) {
		if (type.isArray()) {
			return "table{" + typeName(type.getComponentType()) + "}";
		}
		else if (type == String.class)
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
