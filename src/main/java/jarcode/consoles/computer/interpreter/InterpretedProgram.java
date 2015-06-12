package jarcode.consoles.computer.interpreter;

import com.google.common.base.Joiner;
import jarcode.consoles.Consoles;
import jarcode.consoles.computer.*;
import jarcode.consoles.computer.bin.MakeDirectoryProgram;
import jarcode.consoles.computer.bin.TouchProgram;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.interpreter.libraries.Libraries;
import jarcode.consoles.computer.interpreter.types.*;
import jarcode.consoles.internal.ConsoleButton;
import jarcode.consoles.internal.ConsoleFeed;
import jarcode.consoles.util.Position2D;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static jarcode.consoles.computer.ProgramUtils.*;

/*

This class handles the creation of the LuaVM and provides a large
amount of base Lua function bindings.

 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class InterpretedProgram {

	private static final Charset CHARSET = Charset.forName("UTF-8");

	static {
		Libraries.init();
	}

	/**
	 * Executes a lua program from the plugin folder, on a specific computer.
	 *
	 * @param path the path to the program, relative to the plugin folder
	 * @param computer the comptuer to run the program on
	 */
	public static void execFile(String path, Computer computer) {
		File file = new File(Consoles.getInstance().getDataFolder().getAbsolutePath()
				+ File.separatorChar + path);
		if (!file.exists() || file.isDirectory()) {
			return;
		}
		try {
			String program = FileUtils.readFileToString(file, CHARSET);
			exec(program, computer);
		}
		catch (IOException e) {
			Consoles.getInstance().getLogger().warning("Failed to read lua program from plugin folder: " + path);
			e.printStackTrace();
		}
	}

	/**
	 * Compiles and runs the given Lua program. The program is ran
	 * with elevated permissions and does not have access to graphics
	 * APIs.
	 *
	 * This is not suitable for constant execution of Lua code, as it
	 * has to compile and sandbox the code each time.
	 *
	 * The program that is ran will occupy the current terminal instance
	 * for the computer.
	 *
	 * @param program the string that contains the Lua program
	 * @param computer the computer to run the program on
	 * @return true if the program was executed, false if the terminal was busy
	 */
	public static boolean exec(String program, Computer computer) {
		if (computer.getCurrentTerminal().isBusy())
			return false;
		InterpretedProgram inst = new InterpretedProgram();
		inst.restricted = false;
		LinkedStream stream = new LinkedStream();
		inst.compileAndExecute(stream.createOutput(), null, "", computer, null, program);
		computer.getCurrentTerminal().setIO(stream, null, ConsoleFeed.UTF_ENCODER);
		computer.getCurrentTerminal().startFeed();
		stream.close();
		return true;
	}

	public Map<Integer, LuaFrame> framePool = new HashMap<>();
	
	private FSFile file;
	private String path;
	private InputStream in;
	private OutputStream out;
	private Computer computer;
	private ProgramInstance instance;
	private FuncPool pool;
	private String args;
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private List<Integer> allocatedSessions = new ArrayList<>();
	private EmbeddedGlobals globals;

	private InterruptLib interruptLib = new InterruptLib(this::terminated);

	private List<String> registeredChannels = new ArrayList<>();

	private boolean restricted = true;

	// normal constructor for loading a program from a computer
	public InterpretedProgram(FSFile file, String path) {
		this.file = file;
		this.path = path;
	}

	// blank for program instances that need to be setup for Lua programs
	// initiated through Java code
	private InterpretedProgram() {}

	private void map() {

		// instance lua functions, these will call with the program context in mind
		// maps all functions that start with lua$
		Lua.find(this, pool);

		// configurable API options

		if (Consoles.componentRenderingEnabled && isUserExecuted()) {
			Lua.put(this::lua_registerPainter, "paint", pool);
		}

		if (Consoles.frameRenderingEnabled && isUserExecuted()) {
			Lua.put(this::lua_screenBuffer, "screenBuffer", pool);
			Lua.put(this::lua_screenFrame, "screenFrame", pool);
		}
	}

	// used to run programs from a file in a computer
	public void run(OutputStream out, InputStream in, String str, Computer computer,
	                ProgramInstance instance) throws Exception {
		// read from the program file and write it to a buffer
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		try (InputStream is = file.createInput()) {
			int i;
			while (true) {
				if (terminated())
					break;
				if (is.available() > 0 || is instanceof ByteArrayInputStream) {
					i = is.read();
					if (i == -1) break;
					buf.write(i);
				} else Thread.sleep(50);
			}
			if (terminated())
				print(" [PARSE TERMINATED]");
		}

		// parse as a string
		String raw = new String(buf.toByteArray(), CHARSET);

		compileAndExecute(out, in, str, computer, instance, raw);
	}
	// runs a program with the given raw text
	public void compileAndExecute(OutputStream out, InputStream in, String str, Computer computer,
	                ProgramInstance instance, String raw) {
		try {
			this.in = in;
			this.out = out;
			this.computer = computer;
			this.instance = instance;
			this.args = str;

			// if the file is null, something went wrong
			if (file == null) {
				print("null file");
				return;
			}

			// this is our function pool, which are a bunch of LuaFunctions mapped to strings
			// we also use this pool to identify our program with this thread.
			//
			// all static functions that were already mapped are automatically added to this pool
			pool = new FuncPool(Thread.currentThread(), this);

			// map functions from this program instance to the pool
			map();

			// create our globals for Lua. We use a special kind of globals
			// that allows us to finalize variables.
			globals = new EmbeddedGlobals();

			// Load libraries from LuaJ. I left a bunch of libraries from the
			// JSE standards to have less possibilities for users to exploit
			// them.
			globals.load(new JseBaseLib());
			globals.load(new PackageLib());
			globals.load(new Bit32Lib());
			globals.load(new TableLib());
			globals.load(new StringLib());
			globals.load(new BaseLib());

			// I added a missing function to the math library
			globals.load(new EmbeddedMathLib());

			// Load our debugging library, which is used to terminate the program
			globals.load(interruptLib);

			// Load any extra libraries, these can be registered by other plugins
			// Note, we only register libraries that are not restricted.
			Lua.libraries.values().stream()
					.filter((lib) -> !lib.isRestricted || !restricted)
					.forEach((lib) -> globals.load(lib.buildLibrary()));

			// install
			LoadState.install(globals);
			LuaC.install(globals);

			// load functions from our pool
			for (Map.Entry<String, LibFunction> entry : pool.functions.entrySet()) {
				globals.set(entry.getKey(), entry.getValue());
			}

			// set stdout
			if (out == null)
				globals.STDOUT = dummyPrintStream();
			else
				globals.STDOUT = new PrintStream(out);

			// we handle errors with exceptions, so this will always be a dummy writer.
			globals.STDERR = dummyPrintStream();

			// set stdin
			if (in == null)
				globals.STDIN = dummyInputStream();
			else
				globals.STDIN = in;

			// finalize all entries. This means programs cannot modify any created
			// globals at this point.
			globals.finalizeEntries();

			// our main program chunk
			LuaValue chunk;

			// the exit function, if it exists.
			LuaValue exit = null;

			try {

				// try to load in the program
				// this will try to compile the Lua string into Java bytecode
				chunk = globals.load(raw);

			}
			// if we run into a compile error, print out the details and exit.
			catch (LuaError err) {
				if (Consoles.debug)
					err.printStackTrace();
				println("lua:" + ChatColor.RED + " compile error");
				String msg = Arrays.asList(err.getMessage().split("\n")).stream()
						.map(this::warning)
						.collect(Collectors.joining("\n"));
				print(msg);
				return;
			}
			// if we got to this point, the program compiled just fine.
			try {

				// Call the main chunk. This will start executing the Lua program
				// in this thread, and will return when the chunk has been completely
				// executed.
				chunk.call();

				// After this point, we can call the main method since the chunk was
				// just called, and all the methods in said chunk have been declared.

				// get our main function
				LuaValue value = globals.get("main");

				// set the exit function
				exit = globals.get("exit");

				// if the main function exists, call it.
				//
				// some programs won't have a main method. That's fine, in that case
				// most of the code will be in the chunk itself.
				if (value.isfunction()) {
					value.call(args);
				}
			}
			// if the program was interrupted by our debug/interrupt lib
			catch (ProgramInterruptException ex) {
				print("\nProgram terminated");
			}
			// if we encountered an error, we go through quite the process to handle it
			catch (LuaError err) {
				handleLuaError(err);
			}
			// regardless if we encountered an error or not, we try to call our exit function.
			finally {
				// if the exit function exists, and our program has not been terminated
				if (exit != null && exit.isfunction() && !terminated()) {

					try {

						// call the function
						exit.call();

					}
					// again, if the exit function was interrupted.
					catch (ProgramInterruptException ex) {
						print("\nExit routine terminated");
					}
					// if there was an error, handle it the same way.
					catch (LuaError err) {
						handleLuaError(err);
					}
				}
			}
		}
		// cleanup code
		finally {

			// unregister our pool
			if (pool != null)
				pool.cleanup();

			// clear all frame references (from the Lua graphics API)
			framePool.clear();

			// remove all registered channels (from the Lua networking API)
			registeredChannels.forEach(computer::unregisterMessageListener);
			registeredChannels.clear();

			// remove terminal hooks
			Terminal terminal = computer.getTerminal(this);
			if (terminal != null) {
				// user input handling
				terminal.setHandlerInterrupt(null);
				// hook to remove termination from users other than the owner
				terminal.setIgnoreUnauthorizedSigterm(false);
			}

			// remove all components that were set to any screen session(s)
			for (int i : allocatedSessions) {
				computer.setComponent(i, null);
			}
		}

		// at the end of this method, in one way or another, our Lua program will have ended.
	}

	private InputStream dummyInputStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				return 0;
			}
		};
	}

	private PrintStream dummyPrintStream() {
		return new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {}
		}) {
			@Override
			public void println(String x) {}

			@Override
			public void println(Object x) {}
		};
	}

	// finds a new id for a frame that is not taken
	private int findFrameId() {
		int i = 0;
		while(framePool.containsKey(i))
			i++;
		return i;
	}
	public Computer getComputer() {
		return computer;
	}
	private void handleLuaError(LuaError err) {

		// print stack trace in console if in debug mode
		if (Consoles.debug)
			Consoles.getInstance().getLogger().severe("\n" + ExceptionUtils.getFullStackTrace(err));

		// start of our error string
		String errorBreakdown = err.getMessage();

		// We check for errors that caused the top-level error we just encountered, and add their
		// information to the breakdown.
		boolean cont;
		do {
			cont = false;
			if (err.getCause() instanceof LuaError) {
				errorBreakdown += "\n\nCaused by:\n" + err.getCause().getMessage();
				cont = true;
			}
			else if (err.getCause() instanceof InvocationTargetException) {
				if (((InvocationTargetException) err.getCause()).getTargetException() != null)
					errorBreakdown += "\n\nCaused by:\n"
							+ ((InvocationTargetException) err.getCause()).getTargetException().getClass().getSimpleName()
							+ ": " + ((InvocationTargetException) err.getCause()).getTargetException().getMessage();
				if (((InvocationTargetException) err.getCause()).getTargetException() instanceof LuaError) {
					err = (LuaError) ((InvocationTargetException) err.getCause()).getTargetException();
					cont = true;
				}
			}
		}
		while (cont);

		// tell the user we encountered a runtime error
		println("lua:" + ChatColor.RED + " runtime error");

		// split into lines and color each line
		String[] arr = Arrays.asList(errorBreakdown.split("\n")).stream()
				.map(this::err)
				.toArray(String[]::new);
		// combine again
		String msg = Joiner.on('\n').join(arr);

		// if the amount of lines in the breakdown is greater than 16, dump it to a file
		if (arr.length > 16 && isUserExecuted()) {

			// get terminal instance
			Terminal terminal = computer.getTerminal(this);
			if (terminal != null) {

				// find file name to use. We use LuaFile because
				// it's a lot easier than using FSFile.
				LuaFile file;
				int i = 0;
				while (resolve("lua_dump" + i) != null) {
					i++;
				}
				file = lua$touch("lua_dump" + i);
				assert file != null;

				// grab Lua version
				String version;
				try {
					version = globals.get("_VERSION").checkjstring();
				}
				catch (LuaError ignored) {
					version = "?";
				}

				// prefix and transform message
				msg = "Lua stack trace from " + format.format(new Date(System.currentTimeMillis())) + "\n"
						+ "Lua version: " + version + "\n\n"
						+ ChatColor.stripColor(msg.replace("\t", "    "));

				// write the data to the file
				file.write(msg);

				// tell the user we dumped the error breakdown to a file
				String cd = terminal.getCurrentDirectory();
				if (cd.endsWith("/"))
					cd = cd.substring(0, cd.length() - 1);
				println("lua:" + ChatColor.RED + " stack trace too large!");
				print("lua:" + ChatColor.RED + " dumped: " + ChatColor.YELLOW + cd + "/" + "lua_dump" + i);
			}
		}
		// if small enough, print the error directly in the terminal
		else
			print(msg);
	}
	private String warning(String str) {
		return "\t" + ChatColor.YELLOW + str;
	}
	private String err(String str) {
		return "\t" + ChatColor.RED + str;
	}
	protected void print(String formatted) {
		try {
			out.write(formatted.getBytes(CHARSET));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected void println(String formatted) {
		print(formatted + '\n');
	}
	protected void nextLine() {
		print("\n");
	}
	protected FSBlock resolve(String input) {
		return computer.resolve(input, this);
	}
	protected void terminate() {
		if (instance != null) instance.terminate();
	}
	protected boolean terminated() {
		return instance != null && instance.isTerminated();
	}
	protected boolean isUserExecuted() {
		return instance != null;
	}
	protected void sleepFor(long ms) {
		if (isUserExecuted()) sleepFor(ms);
	}

	//
	// Below are all functions visible to the Lua program (identified by the 'lua$' prefix)
	//

	public LuaTypeBuilder lua$typeBuilder() {
		return new LuaTypeBuilder();
	}

	public LuaValue lua$defineType(LuaValue value) {
		if (value.isuserdata() && value.checkuserdata() instanceof LuaTypeBuilder)
			return LuaTypeBuilder.define((LuaTypeBuilder) value.checkuserdata());
		else return LuaValue.NIL;
	}

	public LuaValue lua$loadstring(String arg) {
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

	public void lua$removeRestrictions() {
		if (!restricted || !isUserExecuted()) return;

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
			}
			else {
				print("\nlua: insufficient permissions");
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
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	private String lua$read() {
		final String[] result = {null};
		AtomicBoolean locked = new AtomicBoolean(true);
		computer.getTerminal(this).setHandlerInterrupt((str) -> {
			result[0] = str;
			locked.set(false);
		});
		try {
			while (locked.get() && !terminated()) {
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result[0];
	}
	private void lua$nextLine() {
		print("\n");
	}
	private String lua$owner() {
		return computer.getOwner().toString();
	}
	private String lua$hostname() {
		return computer.getHostname();
	}
	private LuaComputer lua$findComputer(String hostname) {
		if (!isUserExecuted()) return null;
		sleepFor(40);
		Computer computer = ComputerHandler.getInstance().find(hostname);
		if (computer == null) return null;
		else return new LuaComputer(computer);
	}
	private LuaChannel lua$registerChannel(String channel) {
		if (!isUserExecuted()) return null;
		sleepFor(40);
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
	private LuaTerminal lua$getTerminal() {
		return new LuaTerminal(computer.getTerminal(this));
	}
	private LuaValue lua$static_arr(int size) {
		return new LuaArray(size);
	}
	private void lua$ignoreTerminate(Boolean ignore) {
		if (!isUserExecuted()) return;
		sleepFor(20);
		getComputer().getTerminal(this).setIgnoreUnauthorizedSigterm(ignore);
	}
	private String[] lua$soundList() {
		sleepFor(20);
		return Arrays.asList(Sound.values()).stream()
				.map(Enum::name)
				.toArray(String[]::new);
	}
	private void lua$sound(String name, LuaValue v1, LuaValue v2) {
		sleepFor(20);
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
	private Boolean lua$clear() {
		sleepFor(50);
		return schedule(() -> {
			Terminal terminal = computer.getTerminal(this);
			if (terminal != null)
				terminal.clear();
			return true;
		}, this::terminated);
	}
	private String lua$programDir() {
		if (path == null) return null;
		String[] arr = this.path.split("/");
		return Arrays.asList(arr).stream()
				.limit(arr.length - 1)
				.collect(Collectors.joining("/"));
	}
	private String lua$programPath() {
		return path;
	}
	private LuaValue lua$require(String path) {
		sleepFor(10);
		FSBlock block = computer.getBlock(path, "/lib");
		LuaFile file = block instanceof FSFile ? new LuaFile((FSFile) block, path,
				"/lib", this::terminated, computer) : null;
		if (file == null) {
			String[] arr = this.path.split("/");
			String programDir = Arrays.asList(arr).stream()
					.limit(arr.length - 1)
					.collect(Collectors.joining("/"));
			block = computer.getBlock(path, programDir);
			file = block instanceof FSFile ? new LuaFile((FSFile) block, path,
					programDir, this::terminated, computer) : null;
			if (file == null) {
				println("lua:" + ChatColor.RED + " failed to load '" + path + "', doesn't exist");
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
			println("lua:" + ChatColor.RED + " failed to compile '" + path + "'");
			return null;
		}
		return value.call();
	}
	private LuaBuffer lua_screenBuffer(Integer index) {
		sleepFor(20);
		index--;
		if (!computer.screenAvailable(index)) return null;
		allocatedSessions.add(index);
		BufferedFrameComponent component = new BufferedFrameComponent(computer);
		computer.setComponent(index, component);
		return new LuaBuffer(this, index, component, interruptLib::update);
	}
	private void lua$write(String text) {
		print(text);
	}
	private LuaPainter lua_registerPainter(Integer index, FunctionBind painter, FunctionBind listener, Integer bg) {
		index--;
		if (!computer.screenAvailable(index)) return null;
		computer.registerPainter(index, (g, context) -> painter.call(g, context),
				(x, y, context) -> listener.call(x, y, context), bg);
		allocatedSessions.add(index);
		return new LuaPainter(index, computer);
	}
	private String lua$args() {
		return args;
	}
	private LuaFolder lua$resolveFolder(String path) {
		sleepFor(10);
		FSBlock block = resolve(path);
		return block instanceof FSFolder ? new LuaFolder((FSFolder) block, path,
				computer.getTerminal(this).getCurrentDirectory(), this::terminated, computer) : null;
	}
	private LuaFile lua$resolveFile(String path) {
		sleepFor(10);
		FSBlock block = resolve(path);
		return block instanceof FSFile ? new LuaFile((FSFile) block, path,
				computer.getTerminal(this).getCurrentDirectory(), this::terminated, computer) : null;
	}
	private LuaFrame lua_screenFrame() {
		if (framePool.size() > 128) return null;
		int id = findFrameId();
		LuaFrame frame = new LuaFrame(id, computer, () -> framePool.remove(id));
		framePool.put(id, frame);
		return frame;
	}
	private int lua$chestList() {
		sleepFor(40);
		return ComputerHandler.findChests(computer).length;
	}
	private LuaValue lua$getChest(int index) throws InterruptedException {
		sleepFor(40);
		Chest[] chests = schedule(() -> ComputerHandler.findChests(computer), this::terminated);
		if (index >= chests.length || index < 0) return LuaValue.NIL;
		LuaChest lua = new LuaChest(chests[index], this::terminated);
		return CoerceJavaToLua.coerce(lua);
	}
	private LuaFile lua$touch(String path) {
		sleepFor(10);
		FSFile file = new TouchProgram(false).touch(path, computer, computer.getTerminal(this));
		return file != null ? new LuaFile(file, path,
				computer.getTerminal(this).getCurrentDirectory(), this::terminated, computer) : null;
	}
	private String[] lua$reflect() {
		return pool.functions.keySet().toArray(new String[pool.functions.size()]);
	}
	private void lua$sleepFor(Integer ms) {
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
	@SuppressWarnings("SpellCheckingInspection")
	private LuaFolder lua$mkdir(String path) {
		sleepFor(10);
		FSFolder folder = new MakeDirectoryProgram(false).mkdir(path, computer, computer.getTerminal(this));
		return folder != null ? new LuaFolder(folder, path,
				computer.getTerminal(this).getCurrentDirectory(), this::terminated, computer) : null;
	}
	@SuppressWarnings("SpellCheckingInspection")
	private void lua$printc(String formatted) {
		formatted = ChatColor.translateAlternateColorCodes('&', formatted);
		println(formatted);
	}
}
