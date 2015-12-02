package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.ascript.interfaces.ScriptError;
import ca.jarcode.ascript.interfaces.ScriptValue;
import ca.jarcode.consoles.Lang;
import ca.jarcode.ascript.Script;
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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static ca.jarcode.consoles.Lang.lang;
import static ca.jarcode.consoles.computer.ProgramUtils.handleBlockCreate;
import static ca.jarcode.consoles.computer.ProgramUtils.schedule;

@SuppressWarnings({"unused"})

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

	@Override
	protected void map() {
		// instance lua functions, these will call with the program context in mind
		// maps all functions that start with $
		Script.find(this, pool);

		// configurable API options

		// the below function is terrible, and it's permemently disabled now.
		// Lua.put(this::lua_registerPainter, "paint", pool);

		if (Computers.frameRenderingEnabled) {
			Script.unsafeMap(this::lua_screenBuffer, "screenBuffer", pool);
			Script.unsafeMap(this::lua_screenFrame, "screenFrame", pool);
		}
	}

	@FunctionManual("Evalutes Lua code from a string passed as an argument. This function will " +
			"return a value if the compiled chunk returns something, otherwise it will return " +
			"an error string.")
	@Deprecated
	public ScriptValue _UNUSED_$loadstring(
			@Arg(name = "arg", info = "valid lua code in plaintext") String arg) {
		ScriptValue value;
		try {
			value = globals.load(arg);
		}
		catch (ScriptError err) {
			if (Computers.debug)
				err.printStackTrace();
			return globals.getValueFactory().translate(err.getMessage(), globals);
		}
		return globals.getFunctionFactory().createFunction(value::call).getAsValue();
	}

	@FunctionManual("Removes restrictions on the current running program, once authenticated with " +
			"an admin. A dialog will open prompting for a player with the permission computer.admin " +
			"to verify the function call. Any user may choose to exit the program via the same dialog, " +
			"and a player with insufficient permissions attempting to verify the call will exit the " +
			"program.")
	public void $removeRestrictions() {
		if (!restricted) return;

		// script values are not thread safe
		AtomicBoolean state = new AtomicBoolean(false);
		AtomicBoolean ret = new AtomicBoolean(false);

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
				ret.set(true);
			} else {
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
			throw new GenericScriptError(e);
		}
		if (ret.get()) {
			restricted = false;
			Script.LIBS.values().stream()
					.filter((lib) -> lib.isRestricted)
					.forEach(globals::load);

			globals.removeRestrictions();
		}
	}
	@FunctionManual("Reads input from the terminal that the current program is running in. This function " +
			"will block until input is recieved after the function call.")
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	public String $read() {
		final String[] result = {null};
		AtomicBoolean locked = new AtomicBoolean(true);
		contextTerminal.setHandlerInterrupt((str) -> {
			result[0] = str;
			locked.set(false);
		});
		try {
			while (locked.get() && !terminated()) {
				Thread.sleep(10);
				globals.resetInterrupt();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result[0];
	}
	@FunctionManual("Prints a new line to the console.")
	public void $nextLine() {
		print("\n");
	}
	@FunctionManual("Returns the UUID of the computer's owner as a string.")
	public String $owner() {
		return computer.getOwner().toString();
	}
	@FunctionManual("Returns the computer's hostname as a string")
	public String $hostname() {
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
	public int $wget(
			@Arg(name="url",info="URL of the file to downlaod") String url,
			@Arg(name="path",info="path to download the file to") String path,
			@Arg(name="overwrite",info="whether to overwrite the file if it exists") boolean overwrite) {
		ProgramUtils.PreparedBlock block = handleBlockCreate(path, null, contextTerminal, overwrite);
		if (block.err == null) {
			int ret = WGetProgram.invoke(path, block.blockParent, block.blockName,
					null, terminated, contextTerminal);
			if (ret == 0) return 0;
			else return ret - 5;
		}
		else return block.err.code;
	}
	@FunctionManual("Finds a computer with the given hostname.")
	public LuaComputer $findComputer(
			@Arg(name="hostname",info="hostname of the target computer") String hostname) {
		delay(40);
		Computer computer = ComputerHandler.getInstance().find(hostname);
		if (computer == null) return null;
		else return new LuaComputer(computer);
	}
	@FunctionManual("Registers a network channel that a program can use to send data to other computers.")
	public LuaChannel $registerChannel(
			@Arg(name="channel",info="name of the channel to register") String channel) {
		delay(40);
		if (!computer.isChannelRegistered(channel)) {
			LuaChannel ch = new LuaChannel(globals::resetInterrupt, () -> {
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
	public LuaTerminal $getTerminal() {
		return new LuaTerminal(contextTerminal);
	}
	@FunctionManual("Sets if the program is allowed to be terminated (ignored by the computer's owner).")
	public void $ignoreTerminate(
			@Arg(name="ignore",info="Sets if the program can ignore termination.") Boolean ignore) {
		delay(20);
		contextTerminal.setIgnoreUnauthorizedSigterm(ignore);
	}
	@FunctionManual("Sets if the computer allows the user to switch to other views (ignored by the computer's owner).")
	public void $ignoreViewChange(
			@Arg(name="ignore",info="Sets if the program can ignore view switches.") Boolean ignore) {
		delay(20);
		computer.setIgnoreUnauthorizedViewChange(ignore);
	}
	@FunctionManual("Returns the list of possible sounds the computer can play.")
	public String[] $soundList() {
		delay(20);
		return Arrays.asList(Sound.values()).stream()
				.map(Enum::name)
				.toArray(String[]::new);
	}
	@FunctionManual("Plays a sound. A full list of sounds that can be played can be obtained " +
			"by calling soundList().")
	public void $sound(
			@Arg(name="name",info="name of the sound to play") String name,
			@Arg(name="volume",info="volume of the sound") ScriptValue v1,
			@Arg(name="pitch",info="pitch of the sound") ScriptValue v2) {
		delay(20);
		Sound match = Arrays.asList(Sound.values()).stream()
				.filter(s -> s.name().equals(name.toUpperCase()))
				.findFirst().orElseGet(() -> null);
		if (match == null) {
			throw new IllegalArgumentException(name + " is not a qualified sound");
		}
		schedule(() -> computer.getConsole().getLocation().getWorld()
				.playSound(computer.getConsole().getLocation(), match,
						(float) v1.translateDouble(), (float) v2.translateDouble()));
	}
	@FunctionManual("Clears the terminal.")
	public Boolean $clear() {
		delay(50);
		return schedule(() -> {
			Terminal terminal = contextTerminal;
			if (terminal != null)
				terminal.clear();
			return true;
		}, this::terminated);
	}
	@FunctionManual("Returns the directory the program belongs to, as a string.")
	public String $programDir() {
		if (path == null) return null;
		String[] arr = this.path.split("/");
		return Arrays.asList(arr).stream()
				.limit(arr.length - 1)
				.collect(Collectors.joining("/"));
	}
	@FunctionManual("Returns the path to the program's file, as a string.")
	public String $programPath() {
		return path;
	}
	@FunctionManual("Loads the specified file from /lib or from the folder of the current program")
	public ScriptValue $require(
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
		ScriptValue value;
		try {
			value = globals.load(text);
		}
		catch (ScriptError err) {
			if (Computers.debug)
				err.printStackTrace();
			println("lua:" + ChatColor.RED + " " + String.format(lang.getString("lua-require-compile-fail"), path));
			return null;
		}
		ScriptValue result = value.call();
		// release resoureces, because we won't be using
		// this value to reference the loaded chunk anymore
		value.release();
		return result;
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
		return new LuaBuffer(this, index, component, globals::resetInterrupt);
	}
	@FunctionManual("Appends text to the terminal. This will not suffix a newline (\\n) character, " +
			"unlike the print function.")
	public void $write(
			@Arg(name="text",info="text to write to the terminal") String text) {
		print(text);
	}
	@FunctionManual("Appends formatted text to the terminal using minecraft color codes prefixed by " +
			"the & symbol. This will not suffix a newline (\\n) character, " +
			"unlike the printc function.")
	public void $writec(
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
	public String $args() {
		return args;
	}
	@FunctionManual("Attempts to find a folder at the given path, and will return a " +
			"LuaFolder if found (nil if the folder could not be found).")
	public LuaFolder $resolveFolder(
			@Arg(name = "path", info = "path of the folder to find") String path) {
		delay(10);
		FSBlock block = resolve(path);
		return block instanceof FSFolder ? new LuaFolder((FSFolder) block, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Attempts to find a file at the given path, and will return a " +
			"LuaFile if found (nil if the file could not be found).")
	public LuaFile $resolveFile(
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
	public int $chestList() {
		delay(40);
		return ComputerHandler.findChests(computer).length;
	}
	@FunctionManual("Returns a LuaChest at the specified index, and will return nil if the " +
			"index is invalid. Chest indexes start at zero and end at the amount of chests, minus " +
			"one. The amount of chests available can be checked with chestList().")
	public LuaChest $getChest(
			@Arg(name = "index", info = "the index of the chest to obtain") int index) throws InterruptedException {
		delay(40);
		Chest[] chests = schedule(() -> ComputerHandler.findChests(computer), this::terminated);
		if (index >= chests.length || index < 0) return null;
		return new LuaChest(chests[index], this::terminated);
	}

	@FunctionManual("Creates a new file if it does not already exist. A new LuaFile is returned on " +
			"creation of a file, and nil is returned if a file/folder already exists.")
	public LuaFile $touch(
			@Arg(name = "path", info = "the path to the file to create") String path) {
		delay(10);
		FSFile file = new TouchProgram(false).touch(path, contextTerminal);
		return file != null ? new LuaFile(file, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Returns all computer functions (excludes default Lua functions and libraries) as a table of " +
			"strings.")
	public String[] $reflect() {
		return pool.functions.keySet().toArray(new String[pool.functions.size()]);
	}
	@FunctionManual("Halts the program for the specified amount of time, in miliseconds.")
	public void $sleep(
			@Arg(name = "ms", info = "the duration in which to sleep") Integer ms) {
		try {
			globals.resetInterrupt();
			long target = System.currentTimeMillis() + ms;
			while (System.currentTimeMillis() < target && !terminated()) {
				Thread.sleep(8);
			}
			globals.resetInterrupt();
		}
		catch (InterruptedException e) {
			throw new GenericScriptError(e);
		}
	}
	@FunctionManual("Creates a new directory if it does not already exist. A new LuaFolder is returned on " +
			"creation of the folder, and nil is returned if a file/folder already exists.")
	@SuppressWarnings("SpellCheckingInspection")
	public LuaFolder $mkdir(
			@Arg(name = "path", info = "the path to the folder to create") String path) {
		delay(10);
		FSFolder folder = new MakeDirectoryProgram(false).mkdir(path, contextTerminal);
		return folder != null ? new LuaFolder(folder, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Prints formatted text to the terminal, using minecraft color codes prefixed by " +
			"the & symbol.")
	@SuppressWarnings("SpellCheckingInspection")
	public void $printc(
			@Arg(name = "formatted", info = "formatted text to print to the terminal") String formatted) {
		formatted = ChatColor.translateAlternateColorCodes('&', formatted);
		println(formatted);
	}
	@FunctionManual("Prints text to the terminal")
	@SuppressWarnings("SpellCheckingInspection")
	public void $print(
			@Arg(name = "text", info = "formatted text to print to the terminal") String text) {
		println(text);
	}
	@FunctionManual("Returns text using the given key using the server's locale settings")
	public String $i18n(
			@Arg(name = "key", info = "internalization key") String key) {
		return Lang.lang.getString(key);
	}
}
