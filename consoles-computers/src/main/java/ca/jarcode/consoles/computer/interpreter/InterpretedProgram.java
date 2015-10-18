package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.consoles.Consoles;
import ca.jarcode.consoles.Lang;
import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.ComputerHandler;
import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.computer.ProgramUtils;
import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.bin.MakeDirectoryProgram;
import ca.jarcode.consoles.computer.bin.TouchProgram;
import ca.jarcode.consoles.computer.bin.WGetProgram;
import ca.jarcode.consoles.computer.filesystem.FSBlock;
import ca.jarcode.consoles.computer.filesystem.FSFile;
import ca.jarcode.consoles.computer.filesystem.FSFolder;
import ca.jarcode.consoles.computer.interpreter.types.*;
import ca.jarcode.consoles.computer.manual.Arg;
import ca.jarcode.consoles.computer.manual.FunctionManual;
import ca.jarcode.consoles.computer.manual.ManualManager;
import ca.jarcode.consoles.internal.ConsoleButton;
import ca.jarcode.consoles.api.Position2D;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.OsLib;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static ca.jarcode.consoles.Lang.lang;
import static ca.jarcode.consoles.computer.ProgramUtils.handleBlockCreate;
import static ca.jarcode.consoles.computer.ProgramUtils.schedule;

@SuppressWarnings({"unused", "SpellCheckingInspection"})

public final class InterpretedProgram extends SandboxProgram {

	static {
		ManualManager.load(InterpretedProgram.class);
	}

	public InterpretedProgram(FSFile file, String path) {
		super(file, path);
	}
	public InterpretedProgram() {
		super();
	}

	protected void map() {
		// instance lua functions, these will call with the program context in mind
		// maps all functions that start with lua$
		Lua.find(this, pool);

		// configurable API options

		// this is terrible, and it's permemently disabled now.
		// Lua.put(this::lua_registerPainter, "paint", pool);

		if (Computers.frameRenderingEnabled) {
			Lua.put(this::lua_screenBuffer, "screenBuffer", pool);
			Lua.put(this::lua_screenFrame, "screenFrame", pool);
		}
	}


	@FunctionManual("Returns a LuaTypeBuilder, which can be used to build a unique type for " +
			"use in Lua programs. The builder can be used to set various handlers for the type, " +
			"and once defined, will return a function which can be used to create the type.")
	public LuaTypeBuilder lua$typeBuilder() {
		return new LuaTypeBuilder();
	}

	@FunctionManual("Evalutes Lua code from a string passed as an argument. This function will " +
			"return a value if the compiled chunk returns something, otherwise it will return nil.")
	public LuaValue lua$loadstring(
			@Arg(name = "arg", info = "valid lua code in plaintext") String arg) {
		LuaValue value;
		try {
			value = globals.load(arg);
		}
		catch (LuaError err) {
			if (Consoles.debug)
				err.printStackTrace();
			return LuaString.valueOf(err.getMessage());
		}
		return new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				return value.call();
			}
		};
	}

	@FunctionManual("Removes restrictions on the current running program, once authenticated with " +
			"an admin. A dialog will open prompting for a player with the permission computer.admin " +
			"to verify the function call. Any user may choose to exit the program via the same dialog, " +
			"and a player with insufficient permissions attempting to verify the call will exit the " +
			"program.")
	public void lua$removeRestrictions() {
		if (!restricted) return;

		AtomicBoolean state = new AtomicBoolean(false);

		ConsoleButton allow = new ConsoleButton(computer.getConsole(), "Verify");
		allow.setBorder((byte) 114);
		ConsoleButton exit = new ConsoleButton(computer.getConsole(), "Exit");

		Position2D pos = computer.dialog("The current program needs the permission "
				+ ChatColor.RED + "computer.admin" + ChatColor.BLACK + " to continue", allow, exit);

		exit.addEventListener(event -> {
			computer.getConsole().removeComponent(pos);
			terminate();
			state.set(true);
		});
		allow.addEventListener(event -> {
			computer.getConsole().removeComponent(pos);
			if (event.getPlayer().hasPermission("computer.admin")) {

				restricted = false;
				Lua.libraries.values().stream()
						.filter((lib) -> lib.isRestricted)
						.forEach((lib) -> globals.load(lib.buildLibrary()));

				globals.load(new CoroutineLib());
				globals.load(new OsLib());
			}
			else {
				print("\nlua: " + lang.getString("lua-perm"));
				terminate();
			}
			state.set(true);
		});
		try {
			while (!state.get() && !terminated())
				Thread.sleep(80);
		}
		catch (InterruptedException e) {
			throw new LuaError(e);
		}
	}
	@FunctionManual("Reads input from the terminal that the current program is running in. This function " +
			"will block until input is recieved after the function call.")
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	public String lua$read() {
		final String[] result = {null};
		AtomicBoolean locked = new AtomicBoolean(true);
		contextTerminal.setHandlerInterrupt((str) -> {
			result[0] = str;
			locked.set(false);
		});
		try {
			while (locked.get() && !terminated()) {
				Thread.sleep(10);
				interruptLib.update();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result[0];
	}
	@FunctionManual("Prints a new line to the console.")
	public void lua$nextLine() {
		print("\n");
	}
	@FunctionManual("Returns the UUID of the computer's owner as a string.")
	public String lua$owner() {
		return computer.getOwner().toString();
	}
	@FunctionManual("Returns the computer's hostname as a string")
	public String lua$hostname() {
		return computer.getHostname();
	}
	@FunctionManual("Downloads a file from the given URL to the computer's disk using HTTP via the internet." +
			"This function has various return values:\n\n" +
			"\t\t&e-1&f - file exists on disk\n" +
			"\t\t&e-2&f - invalid current directory\n" +
			"\t\t&e-3&f - invalid file name\n" +
			"\t\t&e-4&f - parent folder doesn't exist\n" +
			"\t\t&e-5&f - parent folder isn't a folder\n" +
			"\t\t&e-6&f - malformed URL\n" +
			"\t\t&e-7&f - IO error while downloading\n" +
			"\t\totherwise, this function will return 0")
	public int lua$wget(
			@Arg(name="url",info="URL of the file to downlaod") String url,
			@Arg(name="path",info="path to download the file to") String path,
			@Arg(name="overwrite",info="whether to overwrite the file if it exists") boolean overwrite) {
		ProgramUtils.PreparedBlock block = handleBlockCreate(path, null, contextTerminal, overwrite);
		if (block.err == null) {
			int ret = WGetProgram.invoke(path, (FSFolder) block.blockParent, block.blockName,
					null, terminated, contextTerminal);
			if (ret == 0) return 0;
			else return ret - 5;
		}
		else return block.err.code;
	}
	@FunctionManual("Finds a computer with the given hostname.")
	public LuaComputer lua$findComputer(
			@Arg(name="hostname",info="hostname of the target computer") String hostname) {
		delay(40);
		Computer computer = ComputerHandler.getInstance().find(hostname);
		if (computer == null) return null;
		else return new LuaComputer(computer);
	}
	@FunctionManual("Registers a network channel that a program can use to send data to other computers.")
	public LuaChannel lua$registerChannel(
			@Arg(name="channel",info="name of the channel to register") String channel) {
		delay(40);
		if (!computer.isChannelRegistered(channel)) {
			LuaChannel ch = new LuaChannel(interruptLib::update, () -> {
				computer.unregisterMessageListener(channel);
				registeredChannels.remove(channel);
			}, this::terminated);
			computer.registerMessageListener(channel, ch::append);
			registeredChannels.add(channel);
			return ch;
		}
		else return null;
	}
	@FunctionManual("Returns the terminal that the current program is being executed from.")
	public LuaTerminal lua$getTerminal() {
		return new LuaTerminal(contextTerminal);
	}
	@FunctionManual("Returns a static array for Lua. Static arrays are like normal arrays except they can't be " +
			"resized, can't have non-numerical keys, are not compatable with the '{}' syntax of defining tables, and " +
			"they are a unique type ('static_array')")
	public LuaValue lua$static_arr(
			@Arg(name="size",info="Size of the array to register") int size) {
		return new LuaArray(size);
	}
	@FunctionManual("Sets if the program is allowed to be terminated (ignored by the computer's owner).")
	public void lua$ignoreTerminate(
			@Arg(name="ignore",info="Sets if the program can ignore termination.") Boolean ignore) {
		delay(20);
		contextTerminal.setIgnoreUnauthorizedSigterm(ignore);
	}
	@FunctionManual("Sets if the computer allows the user to switch to other views (ignored by the computer's owner).")
	public void lua$ignoreViewChange(
			@Arg(name="ignore",info="Sets if the program can ignore view switches.") Boolean ignore) {
		delay(20);
		computer.setIgnoreUnauthorizedViewChange(ignore);
	}
	@FunctionManual("Returns the list of possible sounds the computer can play.")
	public String[] lua$soundList() {
		delay(20);
		return Arrays.asList(Sound.values()).stream()
				.map(Enum::name)
				.toArray(String[]::new);
	}
	@FunctionManual("Plays a sound. A full list of sounds that can be played can be obtained " +
			"by calling soundList().")
	public void lua$sound(
			@Arg(name="name",info="name of the sound to play") String name,
			@Arg(name="volume",info="volume of the sound") LuaValue v1,
			@Arg(name="pitch",info="pitch of the sound") LuaValue v2) {
		delay(20);
		Sound match = Arrays.asList(Sound.values()).stream()
				.filter(s -> s.name().equals(name.toUpperCase()))
				.findFirst().orElseGet(() -> null);
		if (match == null) {
			throw new IllegalArgumentException(name + " is not a qualified sound");
		}
		schedule(() -> computer.getConsole().getLocation().getWorld()
				.playSound(computer.getConsole().getLocation(), match,
						(float) v1.checkdouble(), (float) v2.checkdouble()));
	}
	@FunctionManual("Clears the terminal.")
	public Boolean lua$clear() {
		delay(50);
		return schedule(() -> {
			Terminal terminal = contextTerminal;
			if (terminal != null)
				terminal.clear();
			return true;
		}, this::terminated);
	}
	@FunctionManual("Returns the directory the program belongs to, as a string.")
	public String lua$programDir() {
		if (path == null) return null;
		String[] arr = this.path.split("/");
		return Arrays.asList(arr).stream()
				.limit(arr.length - 1)
				.collect(Collectors.joining("/"));
	}
	@FunctionManual("Returns the path to the program's file, as a string.")
	public String lua$programPath() {
		return path;
	}
	@FunctionManual("Loads the specified file from /lib or from the folder of the current program")
	public LuaValue lua$require(
			@Arg(name="module",info="Name of the lua module to load.") String path) {
		delay(10);
		FSBlock block = computer.getBlock(path, "/lib");
		LuaFile file = block instanceof FSFile ? new LuaFile((FSFile) block, path,
				"/lib", this::terminated, computer) : null;
		if (this.path == null) {
			println("lua:" + ChatColor.RED + " " + String.format(lang.getString("lua-require-fail"), path));
			return null;
		}
		if (file == null) {
			String[] arr = this.path.split("/");
			String programDir = Arrays.asList(arr).stream()
					.limit(arr.length - 1)
					.collect(Collectors.joining("/"));
			block = computer.getBlock(path, programDir);
			file = block instanceof FSFile ? new LuaFile((FSFile) block, path,
					programDir, this::terminated, computer) : null;
			if (file == null) {
				println("lua:" + ChatColor.RED + " " + String.format(lang.getString("lua-require-fail"), path));
				return null;
			}
		}
		String text = file.read();
		LuaValue value;
		try {
			value = globals.load(text);
		}
		catch (LuaError err) {
			if (Consoles.debug)
				err.printStackTrace();
			println("lua:" + ChatColor.RED + " " + String.format(lang.getString("lua-require-compile-fail"), path));
			return null;
		}
		return value.call();
	}

	@FunctionManual("Registers and returns a new screen buffer for the screen session. If the given session " +
			"at the index is already in use, this function will return nil.")
	public LuaBuffer lua_screenBuffer(
			@Arg(name = "index", info = "the index of the screen session to use") Integer index) {
		delay(20);
		index--;
		if (!computer.screenAvailable(index)) return null;
		allocatedSessions.add(index);
		BufferedFrameComponent component = new BufferedFrameComponent(computer);
		computer.setComponent(index, component);
		return new LuaBuffer(this, index, component, interruptLib::update);
	}
	@FunctionManual("Appends text to the terminal. This will not suffix a newline (\\n) character, " +
			"unlike the print function.")
	public void lua$write(
			@Arg(name="text",info="text to write to the terminal") String text) {
		print(text);
	}
	@FunctionManual("Appends formatted text to the terminal using minecraft color codes prefixed by " +
			"the & symbol. This will not suffix a newline (\\n) character, " +
			"unlike the printc function.")
	public void lua$writec(
			@Arg(name="formatted",info="formatted text to write to the terminal") String text) {
		print(ChatColor.translateAlternateColorCodes('&', text));
	}
	public LuaPainter lua_registerPainter(Integer index, FunctionBind painter, FunctionBind listener, Integer bg) {
		index--;
		if (!computer.screenAvailable(index)) return null;
		computer.registerPainter(index, (g, context) -> painter.call(g, context),
				(x, y, context) -> listener.call(x, y, context), bg);
		allocatedSessions.add(index);
		return new LuaPainter(index, computer);
	}
	@FunctionManual("Returns the arguments passed to the program as a single string")
	public String lua$args() {
		return args;
	}
	@FunctionManual("Attempts to find a folder at the given path, and will return a " +
			"LuaFolder if found (nil if the folder could not be found).")
	public LuaFolder lua$resolveFolder(
			@Arg(name = "path", info = "path of the folder to find") String path) {
		delay(10);
		FSBlock block = resolve(path);
		return block instanceof FSFolder ? new LuaFolder((FSFolder) block, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Attempts to find a file at the given path, and will return a " +
			"LuaFile if found (nil if the file could not be found).")
	public LuaFile lua$resolveFile(
			@Arg(name = "path", info = "path of the file to find") String path) {
		delay(10);
		FSBlock block = resolve(path);
		return block instanceof FSFile ? new LuaFile((FSFile) block, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Allocates and creates a frame to use for graphics rendering. This function will " +
			"return a LuaFrame, unless there are more than 128 active frames from the current program.")
	public LuaFrame lua_screenFrame() {
		if (framePool.size() > 128) return null;
		int id = findFrameId();
		LuaFrame frame = new LuaFrame(id, computer, () -> framePool.remove(id));
		framePool.put(id, frame);
		return frame;
	}
	@FunctionManual("Returns the amount of chests behind the computer.")
	public int lua$chestList() {
		delay(40);
		return ComputerHandler.findChests(computer).length;
	}
	@FunctionManual("Returns a LuaChest at the specified index, and will return nil if the " +
			"index is invalid. Chest indexes start at zero and end at the amount of chests, minus " +
			"one. The amount of chests available can be checked with chestList().")
	public LuaChest lua$getChest(
			@Arg(name = "index", info = "the index of the chest to obtain") int index) throws InterruptedException {
		delay(40);
		Chest[] chests = schedule(() -> ComputerHandler.findChests(computer), this::terminated);
		if (index >= chests.length || index < 0) return null;
		return new LuaChest(chests[index], this::terminated);
	}

	@FunctionManual("Creates a new file if it does not already exist. A new LuaFile is returned on " +
			"creation of a file, and nil is returned if a file/folder already exists.")
	public LuaFile lua$touch(
			@Arg(name = "path", info = "the path to the file to create") String path) {
		delay(10);
		FSFile file = new TouchProgram(false).touch(path, contextTerminal);
		return file != null ? new LuaFile(file, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Returns all computer functions (excludes default Lua functions and libraries) as a table of " +
			"strings.")
	public String[] lua$reflect() {
		return pool.functions.keySet().toArray(new String[pool.functions.size()]);
	}
	@FunctionManual("Halts the program for the specified amount of time, in miliseconds.")
	public void lua$sleep(
			@Arg(name = "ms", info = "the duration in which to sleep") Integer ms) {
		try {
			interruptLib.update();
			long target = System.currentTimeMillis() + ms;
			while (System.currentTimeMillis() < target && !terminated()) {
				Thread.sleep(8);
			}
			interruptLib.update();
		}
		catch (InterruptedException e) {
			throw new LuaError(e);
		}
	}
	@FunctionManual("Creates a new directory if it does not already exist. A new LuaFolder is returned on " +
			"creation of the folder, and nil is returned if a file/folder already exists.")
	@SuppressWarnings("SpellCheckingInspection")
	public LuaFolder lua$mkdir(
			@Arg(name = "path", info = "the path to the folder to create") String path) {
		delay(10);
		FSFolder folder = new MakeDirectoryProgram(false).mkdir(path, contextTerminal);
		return folder != null ? new LuaFolder(folder, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Prints formatted text to the terminal, using minecraft color codes prefixed by " +
			"the & symbol.")
	@SuppressWarnings("SpellCheckingInspection")
	public void lua$printc(
			@Arg(name = "formatted", info = "formatted text to print to the terminal") String formatted) {
		formatted = ChatColor.translateAlternateColorCodes('&', formatted);
		println(formatted);
	}
	@FunctionManual("Returns text using the given key using the server's locale settings")
	public String lua$i18n(
			@Arg(name = "key", info = "internalization key") String key) {
		return Lang.lang.getString(key);
	}
}
