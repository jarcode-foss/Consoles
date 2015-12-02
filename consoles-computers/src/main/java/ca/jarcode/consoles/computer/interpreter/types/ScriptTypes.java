package ca.jarcode.consoles.computer.interpreter.types;

import ca.jarcode.ascript.Script;
import ca.jarcode.consoles.computer.manual.ManualEntry;
import ca.jarcode.consoles.computer.manual.ManualManager;
import ca.jarcode.consoles.computer.manual.TypeManual;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptTypes {

	private static List<String> typeNames = new ArrayList<>();

	static {
		Script.map(ScriptTypes::lua_uniqueTypeNames, "uniqueTypeNames");
		ManualManager.load(ScriptTypes.class);
	}

	public static void init() {
		reg(LuaBlock.class);
		reg(LuaBuffer.class);
		reg(LuaChest.class);
		reg(LuaComputer.class);
		reg(LuaChannel.class);
		reg(LuaFile.class);
		reg(LuaFolder.class);
		reg(LuaFrame.class);
		reg(LuaInteraction.class);
		// reg(LuaPainter.class);
		reg(LuaTerminal.class);
	}
	private static void reg(Class<?> type) {
		ManualManager.loadType(type);
		typeNames.add(type.getSimpleName());
		typeManual(type);
	}
	private static String[] lua_uniqueTypeNames() {
		return typeNames.toArray(new String[typeNames.size()]);
	}
	private static void typeManual(Class type) {
		TypeManual info = (TypeManual) type.getAnnotation(TypeManual.class);
		if (info == null)
			return;
		String usage = info.usage().equals("?") ? null : info.usage();
		String desc = info.value();
		if (usage != null)
			usage = "\n\n" + Arrays.asList(usage.split("\n")).stream()
					.map(str -> "\t\t" + str)
					.collect(Collectors.joining("\n"));
		if (type.getSuperclass() != Object.class)
			desc += "\n\n\t\tInherits functions from " + ChatColor.AQUA + type.getSuperclass().getSimpleName()
					+ ChatColor.WHITE;
		desc += "\n\n\t\tContains functions: " + Arrays.asList(ManualManager.lua_manual_functionNames()).stream()
				.filter(name -> name.startsWith(type.getSimpleName()))
				.map(name -> ChatColor.AQUA + name + ChatColor.WHITE)
				.collect(Collectors.joining(", "));
		ManualManager.TYPE_MANUALS.put(type.getSimpleName(),
				new ManualEntry((name) -> "Manual entry for type: " + ChatColor.GREEN + name, null, desc, null,
						usage, null));
	}
}
