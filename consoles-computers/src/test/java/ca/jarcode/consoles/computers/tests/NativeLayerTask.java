package ca.jarcode.consoles.computers.tests;

import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.computer.NativeLoader;
import ca.jarcode.consoles.computer.interpreter.FuncPool;
import ca.jarcode.consoles.computer.interpreter.Lua;
import ca.jarcode.consoles.computer.interpreter.interfaces.*;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNEngine;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNError;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNImpl;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/*
 * Debugging JNI code is a pain in the ass, especially when you have an entire craftbukkit/spigot server running
 * your JNI code in a plugin, making interrupting SIGSEGV's and other signals nearly impossible because the
 * massive amount of Java code is consequently causing the JVM to do a lot of its signal handling shenanigans.
 *
 * My solution is to run tests for the native layer itself as a JUnit test, without a whole server running under
 * the same process. This makes the amount of bogus SIGSEGV's go down to the point where I can manage it.
 *
 * Other tricks involve setting the pthread name so that it is possible to distinguish during debugging.
 */

@SuppressWarnings("unused")
public class NativeLayerTask {

	public static boolean DEBUG = "true".equals(System.getProperty("debugTests"));
	// catch syscall exit exit_group
	public static String GDB_CMD = Computers.debugHookCommand;

	private static final int LOG_TAB = 69;

	private static final String DONE = "[DONE]";
	private static final String FAIL = "[FAIL]";
	private static final String STARTED = "[STARTED]";

	// getting original stdout, don't want to use JUnit's crap
	OutputStream out = new FileOutputStream(FileDescriptor.out);
	PrintStream stdout = new PrintStream(out);

	Process debuggerProcess;

	FuncPool pool;
	ScriptGlobals globals;
	ScriptValue chunk;

	public int loglen = 0;

	public void init() throws Throwable {

		Computers.debug = true;

		log("Loading tests and setting up engine");

		File library;

		try {
			library = new File(
					System.getProperty("user.dir") + File.separator +
							"target/natives" + File.separator +
							NativeLoader.libraryFormat(NativeLoader.getNativeTarget()).apply("computerimpl")
			);

			assert library.exists();
			assert library.isFile();

			System.load(library.getAbsolutePath());

			LuaNEngine.install(LuaNImpl.JIT);

			// map a lambda vesion of our test function
			Lua.map(this::lua$testFunction, "lambdaTestFunction");

			pool = new FuncPool(() -> globals);

			// register the current thread
			pool.register(Thread.currentThread());
		}
		catch (Throwable e) {
			throw wrapException(e, true);
		}

		logn(DONE);

		if (DEBUG) {
			// catch syscall exit exit_group
			debuggerProcess = attachDebugger(library.getAbsolutePath());
			Thread.sleep(2000);
		}

		log("Building new environment");

		try {
			globals = LuaNEngine.newEnvironment(pool, null, System.in, System.out, -1);

			globals.set(
					globals.getValueFactory().translate("testIntegralValue", globals),
					globals.getValueFactory().translate(42, globals)
			);

			pool.mapStaticFunctions();

			// register any lua functions
			Lua.find(this, pool);

			globals.removeRestrictions(); // we need extra packages
		}
		catch (Throwable e) {
			throw wrapException(e, true);
		}

		logn(DONE);
	}

	public RuntimeException wrapException(Throwable e, boolean failType) {
		if (failType)
			logn(FAIL);
		else stdout.println("TEST: failed during a call or internal error");
		stdout.println(e.getMessage());
		stdout.flush();
		return new RuntimeException(e);
	}

	public void loadAndCallChunk() throws Throwable {

		try {
			// load our program into a string
			String program;

			File tests = new File(
					System.getProperty("user.dir") +
							"/src/test/lua/tests.lua".replace("/", File.separator)
			);

			assert tests.exists();

			try (InputStream is = new FileInputStream(tests)) {
				byte[] buf = new byte[is.available()];
				int i;
				int cursor = 0;
				while (cursor < buf.length) {
					int r = buf.length - cursor;
					cursor += is.read(buf, cursor, 1024 > r ? r : 1024);
				}
				program = new String(buf, StandardCharsets.UTF_8);
			}

			log("Loading program chunk");
			logn(STARTED);
			chunk = globals.load(program);

			log("Calling program chunk");
			logn(STARTED);
			stdout.print("\n");
			chunk.call();
			stdout.print("\n");
		}
		// Discovery: you need to catch errors thrown by the JNI and re-throw them, otherwise strange things
		// happen. This is probably due to how exceptions are instantiated through the JNI.
		catch (Throwable e) {
			throw wrapException(e, false);
		}

	}

	public void loadAndCallTests() {

		try {
			log("Indexing test() function");
			logn(DONE);
			stdout.print("\n");
			ScriptValue testFunction = globals.get(globals.getValueFactory().translate("test", globals));
			stdout.print("\n");

			log("Calling test() function");
			logn(STARTED);
			stdout.print("\n");

			ScriptValue result = testFunction.getAsFunction().call(
					globals.getValueFactory().translate(System.getProperty("user.dir"), globals)
			);
			stdout.print("\n");

			if (result.canTranslateInt()) {
				int i = result.translateInt();
				if (i != 0) {
					throw new RuntimeException("test() return value: " + i);
				}
				logn("test() return value: " + i);
			} else {
				throw new RuntimeException("non-integral response returned from test() lua function");
			}
		}
		catch (ScriptError e) {
			throw wrapException(e, false);
		}
	}

	public void sendToDebugger(String cmd) throws IOException {
		OutputStream out = debuggerProcess.getOutputStream();
		debuggerProcess.getOutputStream().write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
	}

	public void cleanup() throws InterruptedException {
		if (globals != null) {
			globals.close();
		}
		if (pool != null) {
			pool.cleanup();
		}
		if (debuggerProcess != null && debuggerProcess.isAlive()) {
			debuggerProcess.destroyForcibly();
			debuggerProcess.waitFor();
		}
		// give time for the previous GDB instance to exit
		if (DEBUG) {
			Thread.sleep(1000);
		}
	}

	public String lua$testFunction(int arg0, String arg1) {
		stdout.println("J: testFunction sucessfully called from Lua!");
		return "ret: ('" + arg1 + "', " + arg0 + ")";
	}

	/*
	public void lua$write(String str) {
		stdout.println(str);
		stdout.flush();
		if (str.startsWith("PANIC:"))
			throw new LuaNError(str);
	}

	public void lua$print(String str) {
		stdout.println(str);
		stdout.flush();
		if (str.startsWith("PANIC:"))
			throw new LuaNError(str);
	}
	*/

	public void log(String message) {
		stdout.print(message);
		stdout.flush();
		loglen += message.length();
	}

	public void logn(String message) {
		StringBuilder builder = new StringBuilder();
		builder.append(" ");
		for (int t = 0; t < LOG_TAB - (loglen + 2); t++)
			builder.append(".");
		builder.append(" ");
		stdout.print(builder.toString());
		stdout.println(message);
		stdout.flush();
		loglen = 0;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private Process attachDebugger(String fullLibraryPath) {
		String beanName = ManagementFactory.getRuntimeMXBean().getName();
		if (!beanName.contains("@")) {
			throw new IllegalStateException("Unable to find process ID to start debug hook");
		}
		String sub = beanName.split("@")[0];
		try {
			Integer.parseInt(sub);
		}
		catch (NumberFormatException e) {
			throw new IllegalStateException("Unable to parse MX bean to find process ID");
		}

		String command = GDB_CMD;

		Function<String, String> format = NativeLoader.libraryFormat(NativeLoader.getNativeTarget());

		command = command.replace("%I", sub);
		command = command.replace("%N", "debug-" + sub);
		command = command.replace("%E", fullLibraryPath);

		try {
			log("Starting debug hook");
			Process process = new ProcessBuilder()
					.command(command.split(","))
					.start();
			logn(DONE);
			return process;
		} catch (IOException e) {
			throw new RuntimeException("Unable to run debug hook command", e);
		}
	}
}
