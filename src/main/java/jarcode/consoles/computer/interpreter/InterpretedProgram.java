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
import jarcode.consoles.computer.manual.Arg;
import jarcode.consoles.computer.manual.FunctionManual;
import jarcode.consoles.computer.manual.ManualManager;
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
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static jarcode.consoles.computer.ProgramUtils.*;

/**

This class handles the creation of the LuaVM and provides a large
amount of base Lua function bindings.

 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class InterpretedProgram {

	private static final Charset CHARSET = Charset.forName("UTF-8");

	static {
		Libraries.init();
		ManualManager.load(InterpretedProgram.class);
		LuaTypes.init();
	}

	/**
	 * Executes a lua program from the plugin folder, on a specific computer.
	 *
	 * @param path the path to the program, relative to the plugin folder
	 * @param terminal the terminal to run the program on
	 * @param args the arguments for the program
	 * @return true if the program was executed, false if the terminal was busy,
	 * or if something went wrong when loading the file.
	 */
	public static boolean execFile(String path, Terminal terminal, String args) {
		File file = new File(Consoles.getInstance().getDataFolder().getAbsolutePath()
				+ File.separatorChar + path);
		if (Consoles.debug)
			Consoles.getInstance().getLogger().info("Executing file: " + path);
		if (!file.exists() || file.isDirectory()) {
			return false;
		}
		try {
			String program = FileUtils.readFileToString(file, CHARSET);
			return exec(program, terminal, args);
		}
		catch (IOException e) {
			Consoles.getInstance().getLogger().warning("Failed to read lua program from plugin folder: " + path);
			e.printStackTrace();
			return false;
		}
	}

	public static boolean execFile(String path, Terminal terminal) {
		return execFile(path, terminal, "");
	}

	/**
	 * Compiles and runs the given Lua program. The program is ran
	 * with elevated permissions.
	 *
	 * This is not suitable for constant execution of Lua code, as it
	 * has to compile and sandbox the code each time.
	 *
	 * The program that is ran will occupy the current terminal instance
	 * for the computer.
	 *
	 * The directory of the program will be the current directory of
	 * the terminal
	 *
	 * @param program the string that contains the Lua program
	 * @param terminal the terminal to run the program on
	 * @param args the arguments for the program
	 * @return true if the program was executed, false if the terminal was busy
	 */
	public static boolean exec(String program, Terminal terminal, String args) {
		if (terminal.isBusy())
			return false;
		Computer computer = terminal.getComputer();
		InterpretedProgram interpretedProgram = new InterpretedProgram();
		interpretedProgram.restricted = false;
		interpretedProgram.contextTerminal = terminal;
		ProgramInstance instance = new ProgramInstance(interpretedProgram, "", computer, program);

		terminal.setProgramInstance(instance);

		terminal.setIO(instance.in, instance.out, ConsoleFeed.UTF_ENCODER);
		terminal.startFeed();

		instance.start();

		return true;
	}

	public static boolean exec(String program, Terminal terminal) {
		return exec(program, terminal, "");
	}

	public static void pass(String program, Terminal terminal, ProgramInstance instance) {
		pass(program, terminal, instance, "");
	}

	public static void pass(String program, Terminal terminal, ProgramInstance instance, String args) {
		InterpretedProgram inst = new InterpretedProgram();
		inst.restricted = false;
		inst.contextTerminal = terminal;
		instance.interpreted = inst;
		inst.runRaw(instance.stdout, instance.stdin, args, terminal.getComputer(), instance, program);
	}

	public Map<Integer, LuaFrame> framePool = new HashMap<>();
	
	private FSFile file;
	private String path;
	private InputStream in;
	private OutputStream out;
	private Computer computer;
	private FuncPool pool;
	private String args;
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private List<Integer> allocatedSessions = new ArrayList<>();
	private EmbeddedGlobals globals;

	private Runnable terminator;
	private BooleanSupplier terminated;

	private InterruptLib interruptLib = new InterruptLib(this::terminated);

	private List<String> registeredChannels = new ArrayList<>();

	private boolean restricted = true;

	private Terminal contextTerminal = null;

	// normal constructor for loading a program from a computer
	public InterpretedProgram(FSFile file, String path) {
		this.file = file;
		this.path = path;
	}

	// for program instances that need to be setup for Lua programs
	// initiated through Java code
	private InterpretedProgram() {}

	private void map() {

		// instance lua functions, these will call with the program context in mind
		// maps all functions that start with lua$
		Lua.find(this, pool);

		// configurable API options

		if (Consoles.componentRenderingEnabled) {
			Lua.put(this::lua_registerPainter, "paint", pool);
		}

		if (Consoles.frameRenderingEnabled) {
			Lua.put(this::lua_screenBuffer, "screenBuffer", pool);
			Lua.put(this::lua_screenFrame, "screenFrame", pool);
		}
	}

	private void setup(OutputStream out, InputStream in, String str, Computer computer, ProgramInstance instance) {
		this.in = in;
		this.out = out;
		this.computer = computer;
		if (instance != null) {
			this.terminated = instance::isTerminated;
			this.terminator = instance::terminate;
		}
		this.args = str;
	}

	// used to run programs from a file in a computer
	public void run(OutputStream out, InputStream in, String str, Computer computer,
	                ProgramInstance instance) throws Exception {

		setup(out, in, str, computer, instance);

		// if the file is null, something went wrong
		if (file == null) {
			print("null file");
			return;
		}

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

		compileAndExecute(raw);
	}

	public void runRaw(OutputStream out, InputStream in, String str, Computer computer,
	                   ProgramInstance inst, String raw) {
		setup(out, in, str, computer, inst);
		compileAndExecute(raw);
	}

	// runs a program with the given raw text
	public void compileAndExecute(String raw) {
		try {

			if (Consoles.debug)
				Consoles.getInstance().getLogger().info("[DEBUG] compiling and running program " +
						"(charlen: " + raw.length() + ")");

			if (contextTerminal == null) {
				contextTerminal = computer.getTerminal(this);
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

			if (!restricted) {
				globals.load(new CoroutineLib());
				globals.load(new OsLib());
			}

			// install
			LoadState.install(globals);
			LuaC.install(globals);

			// Block some functions
			globals.set("load", LuaValue.NIL);
			globals.set("loadfile", LuaValue.NIL);
			// require should be used instead
			globals.set("dofile", LuaValue.NIL);

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
			Terminal terminal = contextTerminal;
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
		if (arr.length > 16) {

			// get terminal instance
			Terminal terminal = contextTerminal;
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
			throw new LuaError(e);
		}
	}
	protected void println(String formatted) {
		print(formatted + '\n');
	}
	protected void nextLine() {
		print("\n");
	}
	protected FSBlock resolve(String input) {
		return computer.getBlock(input, contextTerminal.getCurrentDirectory());
	}
	protected void terminate() {
		terminator.run();
	}
	protected boolean terminated() {
		return terminated.getAsBoolean();
	}
	protected void delay(long ms) {
		if (restricted) ProgramUtils.sleep(ms);
	}
	public void resetInterrupt() {
		interruptLib.update();
	}

	//
	// Below are all functions visible to the Lua program (identified by the 'lua$' prefix)
	//

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
	@FunctionManual("Reads input from the terminal that the current program is running in. This function " +
			"will block until input is recieved after the function call.")
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	private String lua$read() {
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
	private void lua$nextLine() {
		print("\n");
	}
	@FunctionManual("Returns the UUID of the computer's owner as a string.")
	private String lua$owner() {
		return computer.getOwner().toString();
	}
	@FunctionManual("Returns the computer's hostname as a string")
	private String lua$hostname() {
		return computer.getHostname();
	}
	@FunctionManual("Finds a computer with the given hostname.")
	private LuaComputer lua$findComputer(
		@Arg(name="hostname",info="hostname of the target computer") String hostname) {
		delay(40);
		Computer computer = ComputerHandler.getInstance().find(hostname);
		if (computer == null) return null;
		else return new LuaComputer(computer);
	}
	@FunctionManual("Registers a network channel that a program can use to send data to other computers.")
	private LuaChannel lua$registerChannel(
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
	private LuaTerminal lua$getTerminal() {
		return new LuaTerminal(contextTerminal);
	}
	@FunctionManual("Returns a static array for Lua. Static arrays are like normal arrays except they can't be " + 
		"resized, can't have non-numerical keys, are not compatable with the '{}' syntax of defining tables, and " +
		"they are a unique type ('static_array')")
	private LuaValue lua$static_arr(
		@Arg(name="size",info="Size of the array to register") int size) {
		return new LuaArray(size);
	}
	@FunctionManual("Sets if the program is allowed to be terminated.")
	private void lua$ignoreTerminate(
		@Arg(name="ignore",info="Sets if the program can ignore termination.") Boolean ignore) {
		delay(20);
		contextTerminal.setIgnoreUnauthorizedSigterm(ignore);
	}
	@FunctionManual("Returns the list of possible sounds the computer can play.")
	private String[] lua$soundList() {
		delay(20);
		return Arrays.asList(Sound.values()).stream()
				.map(Enum::name)
				.toArray(String[]::new);
	}
	@FunctionManual("Plays a sound. A full list of sounds that can be played can be obtained " +
			"by calling soundList().")
	private void lua$sound(
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
	private Boolean lua$clear() {
		delay(50);
		return schedule(() -> {
			Terminal terminal = contextTerminal;
			if (terminal != null)
				terminal.clear();
			return true;
		}, this::terminated);
	}
	@FunctionManual("Returns the directory the program belongs to, as a string.")
	private String lua$programDir() {
		if (path == null) return null;
		String[] arr = this.path.split("/");
		return Arrays.asList(arr).stream()
				.limit(arr.length - 1)
				.collect(Collectors.joining("/"));
	}
	@FunctionManual("Returns the path to the program's file, as a string.")
	private String lua$programPath() {
		return path;
	}
	@FunctionManual("Loads the specified file from /lib or from the folder of the current program")
	private LuaValue lua$require(
		@Arg(name="module",info="Name of the lua module to load.") String path) {
		delay(10);
		FSBlock block = computer.getBlock(path, "/lib");
		LuaFile file = block instanceof FSFile ? new LuaFile((FSFile) block, path,
				"/lib", this::terminated, computer) : null;
		if (this.path == null) {
			println("lua:" + ChatColor.RED + " failed to load '" + path + "', doesn't exist");
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

	@FunctionManual("Registers and returns a new screen buffer for the screen session. If the given session " +
			"at the index is already in use, this function will return nil.")
	private LuaBuffer lua_screenBuffer(
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
	private void lua$write(
		@Arg(name="text",info="text to write to the terminal") String text) {
		print(text);
	}
	@FunctionManual("Appends formatted text to the terminal using minecraft color codes prefixed by " +
			"the & symbol. This will not suffix a newline (\\n) character, " +
			"unlike the printc function.")
	private void lua$writec(
			@Arg(name="formatted",info="formatted text to write to the terminal") String text) {
		print(ChatColor.translateAlternateColorCodes('&', text));
	}
	private LuaPainter lua_registerPainter(Integer index, FunctionBind painter, FunctionBind listener, Integer bg) {
		index--;
		if (!computer.screenAvailable(index)) return null;
		computer.registerPainter(index, (g, context) -> painter.call(g, context),
				(x, y, context) -> listener.call(x, y, context), bg);
		allocatedSessions.add(index);
		return new LuaPainter(index, computer);
	}
	@FunctionManual("Returns the arguments passed to the program as a single string")
	private String lua$args() {
		return args;
	}
	@FunctionManual("Attempts to find a folder at the given path, and will return a " +
			"LuaFolder if found (nil if the folder could not be found).")
	private LuaFolder lua$resolveFolder(
			@Arg(name = "path", info = "path of the folder to find") String path) {
		delay(10);
		FSBlock block = resolve(path);
		return block instanceof FSFolder ? new LuaFolder((FSFolder) block, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Attempts to find a file at the given path, and will return a " +
			"LuaFile if found (nil if the file could not be found).")
	private LuaFile lua$resolveFile(
			@Arg(name = "path", info = "path of the file to find") String path) {
		delay(10);
		FSBlock block = resolve(path);
		return block instanceof FSFile ? new LuaFile((FSFile) block, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Allocates and creates a frame to use for graphics rendering. This function will " +
			"return a LuaFrame, unless there are more than 128 active frames from the current program.")
	private LuaFrame lua_screenFrame() {
		if (framePool.size() > 128) return null;
		int id = findFrameId();
		LuaFrame frame = new LuaFrame(id, computer, () -> framePool.remove(id));
		framePool.put(id, frame);
		return frame;
	}
	@FunctionManual("Returns the amount of chests behind the computer.")
	private int lua$chestList() {
		delay(40);
		return ComputerHandler.findChests(computer).length;
	}
	@FunctionManual("Returns a LuaChest at the specified index, and will return nil if the " +
			"index is invalid. Chest indexes start at zero and end at the amount of chests, minus " +
			"one. The amount of chests available can be checked with chestList().")
	private LuaChest lua$getChest(
			@Arg(name = "index", info = "the index of the chest to obtain") int index) throws InterruptedException {
		delay(40);
		Chest[] chests = schedule(() -> ComputerHandler.findChests(computer), this::terminated);
		if (index >= chests.length || index < 0) return null;
		return new LuaChest(chests[index], this::terminated);
	}

	@FunctionManual("Creates a new file if it does not already exist. A new LuaFile is returned on " +
			"creation of a file, and nil is returned if a file/folder already exists.")
	private LuaFile lua$touch(
			@Arg(name = "path", info = "the path to the file to create") String path) {
		delay(10);
		FSFile file = new TouchProgram(false).touch(path, computer, contextTerminal);
		return file != null ? new LuaFile(file, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Returns all computer functions (excludes default Lua functions and libraries) as a table of " +
			"strings.")
	private String[] lua$reflect() {
		return pool.functions.keySet().toArray(new String[pool.functions.size()]);
	}
	@FunctionManual("Halts the program for the specified amount of time, in miliseconds.")
	private void lua$sleep(
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
	private LuaFolder lua$mkdir(
			@Arg(name = "path", info = "the path to the folder to create") String path) {
		delay(10);
		FSFolder folder = new MakeDirectoryProgram(false).mkdir(path, computer, contextTerminal);
		return folder != null ? new LuaFolder(folder, path,
				contextTerminal.getCurrentDirectory(), this::terminated, computer) : null;
	}
	@FunctionManual("Prints formatted text to the terminal, using minecraft color codes prefixed by " +
			"the & symbol.")
	@SuppressWarnings("SpellCheckingInspection")
	private void lua$printc(
			@Arg(name = "formatted", info = "formatted text to print to the terminal") String formatted) {
		formatted = ChatColor.translateAlternateColorCodes('&', formatted);
		println(formatted);
	}
}
