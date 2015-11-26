package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.consoles.CColor;
import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.computer.*;
import ca.jarcode.consoles.computer.bin.TouchProgram;
import ca.jarcode.consoles.computer.filesystem.FSBlock;
import ca.jarcode.consoles.computer.filesystem.FSFile;
import ca.jarcode.consoles.computer.interpreter.func.TwoArgFunc;
import ca.jarcode.consoles.computer.interpreter.interfaces.*;
import ca.jarcode.consoles.computer.interpreter.libraries.Libraries;
import ca.jarcode.consoles.computer.interpreter.types.LuaFile;
import ca.jarcode.consoles.computer.interpreter.types.LuaFrame;
import ca.jarcode.consoles.computer.interpreter.types.ScriptTypes;
import ca.jarcode.consoles.internal.ConsoleFeed;
import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.ChatColor;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ca.jarcode.consoles.Lang.lang;

/**

This class handles the creation and sandbox of the LuaVM

 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public abstract class SandboxProgram {

	public static final Supplier<SandboxProgram> FACTORY = InterpretedProgram::new;
	public static final TwoArgFunc<SandboxProgram, FSFile, String> FILE_FACTORY = InterpretedProgram::new;

	private static final Charset CHARSET = Charset.forName("UTF-8");

	static {
		Libraries.init();
		ScriptTypes.init();
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
		File file = new File(Computers.getInstance().getDataFolder().getAbsolutePath()
				+ File.separatorChar + path);
		if (Computers.debug)
			Computers.getInstance().getLogger().info("Executing file: " + path);
		if (!file.exists() || file.isDirectory()) {
			return false;
		}
		try {
			String program = FileUtils.readFileToString(file, CHARSET);
			return exec(program, terminal, args);
		}
		catch (IOException e) {
			Computers.getInstance().getLogger().warning(String.format(lang.getString("program-load-fail"), path));
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
		SandboxProgram sandboxProgram = FACTORY.get();
		sandboxProgram.restricted = false;
		sandboxProgram.contextTerminal = terminal;
		ProgramInstance instance = new ProgramInstance(sandboxProgram, "", computer, program);

		terminal.setProgramInstance(instance);

		terminal.setIO(instance.in, instance.out, ConsoleFeed.UTF_ENCODER);
		terminal.startFeed();

		instance.start();

		return true;
	}

	public static boolean exec(String program, Terminal terminal) {
		return exec(program, terminal, "");
	}

	public static SandboxProgram pass(String program, Terminal terminal, ProgramInstance instance) {
		return pass(program, terminal, instance, "");
	}

	public static SandboxProgram pass(String program, Terminal terminal, ProgramInstance instance, String args) {
		return pass(FACTORY.get(), program, terminal, instance, args);
	}

	/**
	 * Passes a program instance from provided to an interpreted lua program
	 *
	 * @param inst sandbox instance
	 * @param program raw program text
	 * @param terminal terminal instance
	 * @param instance program instance
	 * @param args lua program arguments
	 * @return the sandbox instance
	 */
	public static SandboxProgram pass(SandboxProgram inst, String program,
	                                  Terminal terminal, ProgramInstance instance, String args) {
		inst.restricted = false;
		inst.contextTerminal = terminal;
		instance.interpreted = inst;
		inst.runRaw(instance.stdout, instance.stdin, args, terminal.getComputer(), instance, program);
		return inst;
	}

	public Map<Integer, LuaFrame> framePool = new HashMap<>();
	
	protected FSFile file;
	protected String path;
	protected InputStream in;
	protected OutputStream out;
	protected Computer computer;
	protected FuncPool pool = new FuncPool(this);
	protected String args;
	protected SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	protected List<Integer> allocatedSessions = new ArrayList<>();
	protected ScriptGlobals globals;

	protected Runnable terminator;
	protected BooleanSupplier terminated;

	protected List<String> registeredChannels = new ArrayList<>();

	protected boolean restricted = true;

	protected Terminal contextTerminal = null;

	protected String defaultChunk = null;

	// normal constructor for loading a program from a computer
	public SandboxProgram(FSFile file, String path) {
		this.file = file;
		this.path = path;
	}

	// for program instances that need to be setup for Lua programs
	// initiated through Java code
	protected SandboxProgram() {}

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
				print(" [" + lang.getString("parse-term") + "]");
		}

		// parse as a string
		String raw = new String(buf.toByteArray(), CHARSET);

		loadDefaultChunk();
		compileAndExecute(raw);
	}

	protected abstract void map();

	private void loadDefaultChunk() {
		FSBlock block = computer.getBlock("/bin/default", "/");
		if (block instanceof FSFile) {
			defaultChunk = new LuaFile((FSFile) block, "/bin/default", "/", this::terminated, computer).read();
		}
	}

	public void runRaw(OutputStream out, InputStream in, String str, Computer computer,
	                   ProgramInstance inst, String raw) {
		setup(out, in, str, computer, inst);
		loadDefaultChunk();
		compileAndExecute(raw);
	}

	// runs a program with the given raw text
	public void compileAndExecute(String raw) {

		/*
		 * It's important to remember how the script abstractions works, if
		 * a ScriptValue is not longer in use, it should be released (everything
		 * is released at the end of the engine's lifecycle). This does not mean
		 * the value itself is invalidated, only the 'handle' for the value.
		 */

		try {

			if (Computers.debug)
				Computers.getInstance().getLogger().info("[DEBUG] compiling and running program " +
						"(charlen: " + raw.length() + ")");

			if (contextTerminal == null) {
				contextTerminal = computer.getTerminal(this);
			}

			// this is our function pool, which are a bunch of LuaFunctions mapped to strings
			// we also use this pool to identify our program with this thread.
			//
			// all static functions that were already mapped are automatically added to this pool
			pool.register(Thread.currentThread());

			// map functions from this program instance to the pool
			map();

			// create our globals for Lua. We use a special kind of globals
			// that allows us to finalize variables.
			globals = ScriptEngine.newEnvironment(pool, terminated, in, out, Computers.scriptHeapSize);

			// Load any extra libraries, these can be registered by other plugins
			// Note, we only register libraries that are not restricted.
			Lua.libraries.values().stream()
					.filter((lib) -> !lib.isRestricted || !restricted)
					.forEach((lib) -> globals.load(lib));

			if (!restricted)
				globals.removeRestrictions();

			// our main program chunk and the default chunk
			ScriptValue chunk, def;

			// the exit function, if it exists.
			ScriptValue exit = null;

			// if there is a default chunk/program to load into the globals, load it!
			if (defaultChunk != null && !defaultChunk.isEmpty()) {

				// try to load the default chunk
				def = loadChunk(defaultChunk);

				// if the previous method returned a chunk, call it!
				if (def != null) try {

					// call the default chunk
					def.call();

					// release resources
					def.release();
				}
				// if the program was interrupted by our debug/interrupt lib
				catch (ProgramInterruptException ex) {

					print("\n" + lang.getString("program-term"));

					// we should return if it was interrupted in the default chunk, cleanup will
					// still be done in the lower-most finally block.
					return;
				}
				// error handling
				//
				// we don't return if we encountered an error in the default chunk,
				// instead we continue and attempt to run the main chunk.
				catch (ScriptError err) {
					handleLuaError(err);
				}
			}

			// try to load the main chunk
			chunk = loadChunk(raw);

			// if the previous method returned null, it didn't compile (and handled the errors)
			// we should just exit from here
			if (chunk == null)
				return;

			// if we got to this point, the program compiled just fine.
			try {

				// Call the main chunk. This will start executing the Lua program
				// in this thread, and will return when the chunk has been completely
				// executed.
				chunk.call();

				// release resources
				chunk.release();

				// After this point, we can call the main method since the chunk was
				// just called, and all the methods in said chunk have been declared.

				// get our main function
				ScriptValue value = globals.get(ValueFactory.getDefaultFactory().translate("main", globals));

				// set the exit function
				exit = globals.get(ValueFactory.getDefaultFactory().translate("exit", globals));

				// if the main function exists, call it.
				//
				// some programs won't have a main method. That's fine, in that case
				// most of the code will be in the chunk itself.
				if (value.isFunction()) {
					value.getAsFunction().call(ValueFactory.getDefaultFactory().translate(args, globals));
				}

				// release resources
				value.release();
			}
			// if the program was interrupted by our debug/interrupt lib
			catch (ProgramInterruptException ex) {
				print("\n" + lang.getString("program-term"));
			}
			// if we encountered an error, we go through quite the process to handle it
			catch (ScriptError err) {
				handleLuaError(err);
			}
			// regardless if we encountered an error or not, we try to call our exit function.
			finally {
				if (exit != null) {
					// if the exit function exists, and our program has not been terminated
					if (exit.isFunction() && !terminated()) {

						try {

							// call the function
							exit.call();

						}
						// again, if the exit function was interrupted.
						catch (ProgramInterruptException ex) {
							print("\n" + lang.getString("exit-func-term"));
						}
						// if there was an error, handle it the same way.
						catch (ScriptError err) {
							handleLuaError(err);
						}
					}
					// release resources
					exit.release();
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
				computer.setIgnoreUnauthorizedViewChange(false);
			}

			// remove all components that were set to any screen session(s)
			for (int i : allocatedSessions) {
				computer.setComponent(i, null);
			}

			// close resources
			if (globals != null)
				globals.close();
		}
	}

	public FuncPool getPool() {
		return pool;
	}

	// loads a raw chunk and returns a LuaValue, handing errors accordingly
	private ScriptValue loadChunk(String raw) {
		ScriptValue chunk;
		try {
			// try to load in the program
			// this will try to compile the Lua string into Java bytecode
			chunk = globals.load(raw);

		}
		// if we run into a compile error, print out the details and exit.
		catch (ScriptError err) {
			if (Computers.debug)
				err.printStackTrace();
			println("lua:" + ChatColor.RED + " " + lang.getString("lua-compile-err"));
			String msg = Arrays.asList(err.getMessage().split("\n")).stream()
					.map(this::warning)
					.collect(Collectors.joining("\n"));
			print(msg);
			return null;
		}
		return chunk;
	}

	// returns a dummy input stream
	public static InputStream dummyInputStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				return 0;
			}
		};
	}

	// returns a dummy print stream
	public static PrintStream dummyPrintStream() {
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
	protected int findFrameId() {
		int i = 0;
		while(framePool.containsKey(i))
			i++;
		return i;
	}

	public Computer getComputer() {
		return computer;
	}

	// handles an uncaught LuaError that occured while attempting to run a lua program
	// this can either print to the terminal, or dump the stack contents to a file (if too large)
	private void handleLuaError(ScriptError err) {

		// print stack trace in console if in debug mode
		if (Computers.debug)
			Computers.getInstance().getLogger().severe("\n" + ExceptionUtils.getFullStackTrace(err));

		// start of our error string
		String errorBreakdown = err.getMessage();

		// We check for errors that caused the top-level error we just encountered, and add their
		// information to the breakdown.
		boolean cont;
		do {
			cont = false;
			if (err.getCause() instanceof ScriptError) {
				errorBreakdown += "\n\n" + lang.getString("lua-dump-cause") +"\n" + err.getCause().getMessage();
				cont = true;
			}
			else if (err.getCause() instanceof InvocationTargetException) {
				if (((InvocationTargetException) err.getCause()).getTargetException() != null)
					errorBreakdown += "\n\n" + lang.getString("lua-dump-cause") + "\n"
							+ ((InvocationTargetException) err.getCause()).getTargetException().getClass().getSimpleName()
							+ ": " + ((InvocationTargetException) err.getCause()).getTargetException().getMessage();
				if (((InvocationTargetException) err.getCause()).getTargetException() instanceof ScriptError) {
					err = (ScriptError) ((InvocationTargetException) err.getCause()).getTargetException();
					cont = true;
				}
			}
		}
		while (cont);

		// tell the user we encountered a runtime error
		println("lua:" + ChatColor.RED + " " + lang.getString("lua-runtime-err"));

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

				FSFile fsfile = new TouchProgram(false).touch("lua_dump" + i, contextTerminal);
				file = new LuaFile(fsfile, path, contextTerminal.getCurrentDirectory(),
						this::terminated, computer);

				// grab Lua version
				String version;
				try {
					version = globals.get(ValueFactory.getDefaultFactory().translate("_VERSION", globals)).translateString();
				}
				catch (ScriptError ignored) {
					version = "?";
				}

				// prefix and transform message
				msg = "Lua stack trace from " + format.format(new Date(System.currentTimeMillis())) + "\n"
						+ "Lua version: " + version + "\n\n"
						+ CColor.strip(msg.replace("\t", "    "));

				// write the data to the file
				file.write(msg);

				// tell the user we dumped the error breakdown to a file
				String cd = terminal.getCurrentDirectory();
				if (cd.endsWith("/"))
					cd = cd.substring(0, cd.length() - 1);
				println("lua:" + ChatColor.RED + " " + lang.getString("lua-dump-size"));
				print("lua:" + ChatColor.RED + " " + String.format(lang.getString("lua-dump-file"),
						ChatColor.YELLOW + cd + "/" + "lua_dump" + i));
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
			throw new GenericScriptError(e);
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
		globals.resetInterrupt();
	}
}
