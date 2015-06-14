package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.interpreter.Lua;
import jarcode.consoles.computer.manual.ManualEntry;
import jarcode.consoles.computer.manual.ManualManager;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LuaTypes {

	private static List<String> typeNames = new ArrayList<>();

	static {
		Lua.map(LuaTypes::lua_uniqueTypeNames, "uniqueTypeNames");
		ManualManager.load(LuaTypes.class);
	}

	public static void init() {
		reg(LuaBlock.class);
		typeManual(LuaBlock.class,
				"A block in the filesystem, which can represent any kind of file or folder. " +
						"This type is abstract and can not be instantiated.", null);
		reg(LuaBuffer.class);
		typeManual(LuaBuffer.class, "A screen buffer used to update the pixels of the screen " +
						"session that it is bound to. Used with LuaFrame to draw and update content.",
				"-- Binds a new buffer to a screen session\n" +
						"local buffer = screenBuffer(index)\n" +
						"-- Switch to the session the buffer was created in\n" +
						"switchSession(index)");
		reg(LuaChest.class);
		typeManual(LuaChest.class, "Represents a chest that this computer has access to.", null);
		reg(LuaComputer.class);
		typeManual(LuaComputer.class, "Represents a computer that exists in the server.", null);
		reg(LuaFile.class);
		typeManual(LuaFile.class, "A stored file that exists in the filesystem.", null);
		reg(LuaFolder.class);
		typeManual(LuaFolder.class, "A folder that exists in the filesystem.", null);
		reg(LuaFrame.class);
		typeManual(LuaFrame.class, "Represents a single frame that can be drawn to the screen. Contains " +
						"methods for drawing various content to the frame, and can be used with LuaBuffer to " +
						"update a session.",
						"-- Creates a new frame\n" +
						"local screen = screenFrame()\n" +
						"-- Draw some text\n" +
						"screen:write(24, 16, \"Hello World!\"\n" +
						"-- Update a LuaBuffer with this frame\n" +
						"buffer:update(screen:id())");
		reg(LuaInteraction.class);
		typeManual(LuaFrame.class, "Represents an interaction on the console screen. Returned from " +
				"LuaBuffer:pollCoords().",
				"-- Poll coordinates from screen buffer\n" +
				"local coords = buffer:pollCoords()\n" +
				"-- Print coordinates if they exist\n" +
				"if coords ~= nil then\n" +
				"\tprint(coords:x() .. \", \" .. coords:y()\n" +
				"end");
		if (Consoles.componentRenderingEnabled)
			reg(LuaPainter.class);
		reg(LuaTerminal.class);
		typeManual(LuaTerminal.class, "Represents the terminal that the current program is running in.",
				"-- Retrieve terminal that the program is running in\n" +
				"local term = getTerminal()");
		reg(LuaTypeBuilder.class);
		typeManual(LuaTypeBuilder.class, "Used to build custom types for Lua programs.", null);
	}
	private static void reg(Class<?> type) {
		ManualManager.loadType(type);
		typeNames.add(type.getSimpleName());
	}
	private static String[] lua_uniqueTypeNames() {
		return typeNames.toArray(new String[typeNames.size()]);
	}
	private static void typeManual(Class type, String desc, String usage) {
		if (usage != null)
			usage = "\n\n" + Arrays.asList(usage.split("\n")).stream()
					.map(str -> "\t\t" + str)
					.collect(Collectors.joining());
		if (type.getSuperclass() != Object.class)
			desc += "\n\nInherits functions from " + ChatColor.GREEN + type.getSuperclass().getSimpleName();
		ManualManager.MANUALS.put(type.getClass().getSimpleName(),
				new ManualEntry((name) -> "Manual entry for type: " + ChatColor.GREEN + name, null, desc, null,
						usage, null));
	}
}
