package ca.jarcode.consoles.computers.tests;

import ca.jarcode.ascript.Joint;
import ca.jarcode.ascript.LibraryCreator;
import ca.jarcode.ascript.interfaces.*;
import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.computer.NativeLoader;
import ca.jarcode.consoles.computer.interpreter.FunctionBind;
import ca.jarcode.ascript.Script;
import ca.jarcode.consoles.computer.interpreter.PartialFunctionBind;
import ca.jarcode.ascript.luanative.LuaNEngine;

import ca.jarcode.ascript.luanative.LuaNImpl;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

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

	private static final String SPLITTER = "+------------------------------------" +
			"-----------------------------------------+";
	private static final String DONE = "[DONE]";
	private static final String FAIL = "[FAIL]";
	private static final String STARTED = "[STARTED]";

	// getting original stdout, don't want to use JUnit's crap
	public static final OutputStream out = new FileOutputStream(FileDescriptor.out);
	public static final PrintStream stdout = new PrintStream(out);

	Process debuggerProcess;

	FuncPool<?> pool;
	ScriptGlobals globals;
	ScriptValue chunk;

	public int loglen = 0;

	static {
		System.setOut(stdout);
		LibraryCreator.link(TestLibrary::new, "test_lib", false);
	}

	// Discovery: you need to catch errors thrown by the JNI and re-throw them, otherwise strange things
	// happen. This is probably due to how exceptions are instantiated through the JNI.
	public void init(boolean sandboxed, boolean install) throws Throwable {

		Computers.debug = true;
		Joint.DEBUG_MODE = true;

		logs("Setting up engine");

		File library = null;

		try {
			if (install) {
				library = new File(
						System.getProperty("user.dir") + File.separator +
								"target/natives" + File.separator +
								NativeLoader.libraryFormat(NativeLoader.getNativeTarget()).apply("computerimpl")
				);

				assert library.exists();
				assert library.isFile();

				System.load(library.getAbsolutePath());

				LuaNEngine.install(sandboxed ? LuaNImpl.JIT : LuaNImpl.JIT_TEST);

				// map a lambda vesion of our test function
				Script.map(this::$testFunction, "lambdaTestFunction");
			}

			pool = new FuncPool<>(() -> globals, () -> false, null);

			// register the current thread
			pool.register(Thread.currentThread());
		}
		catch (Throwable e) {
			throw wrapException(e, true);
		}

		logi(DONE);

		if (DEBUG && install) {
			// catch syscall exit exit_group
			debuggerProcess = attachDebugger(library.getAbsolutePath());
			Thread.sleep(2000);
		}

		log("Building new environment");
		logn(STARTED);

		StringBuilder loaded = new StringBuilder();

		try {
			globals = LuaNEngine.newEnvironment(pool, null, System.in, System.out, -1);

			globals.set(
					globals.getValueFactory().translate("testIntegralValue", globals),
					globals.getValueFactory().translate(42, globals)
			);

			assert globals.get(
					globals.getValueFactory().translate("testIntegralValue", globals)
			).translateInt() == 42;

			pool.mapStaticFunctions();

			// register any lua functions
			Script.find(this, pool);

			loaded.append(pool.functions.entrySet().stream()
					.map((entry) -> entry.getKey() + ": " + valueString(entry.getValue().getAsValue()))
					.collect(Collectors.joining(", ")));

			globals.load(pool);

			Script.LIBS.values().stream()
					.filter((lib) -> !lib.isRestricted || sandboxed)
					.peek((lib) -> {
						loaded.append(", ");
						loaded.append(lib.libraryName);
						loaded.append(" : ");
						loaded.append("library");
					})
					.forEach(globals::load);

			if (!sandboxed)
				globals.removeRestrictions(); // we need extra packages
		}
		catch (Throwable e) {
			throw wrapException(e, false);
		}

		stdout.println("J: loaded: " + loaded);
	}

	public String valueString(ScriptValue value) {
		if (value.isNull())
			return "null";
		else if (value.isFunction())
			return "function";
		else if (value.canTranslateString())
			return "str: '" + value + "'";
		else if (value.canTranslateDouble())
			return Double.toString(value.translateDouble());
		else if (value.canTranslateArray())
			return "table";
		else return "?";
	}

	public RuntimeException wrapException(Throwable e, boolean failType) {
		if (failType)
			logn(FAIL);
		else stdout.println("TEST: failed during a call or internal error");
		stdout.println(e.getMessage());
		stdout.flush();
		return new RuntimeException(e);
	}

	public void loadAndCallChunk(String test) throws Throwable {

		try {
			// load our program into a string
			String program;

			File tests = new File(
					System.getProperty("user.dir") +
							"/src/test/lua/" + test + ".lua".replace("/", File.separator)
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

			logs("Loading program chunk");
			logi(STARTED);
			chunk = globals.load(program, "[test]");

			log("Calling program chunk");
			logn(STARTED);
			stdout.print("\n");
			chunk.call();
			stdout.print("\n");
		}
		catch (Throwable e) {
			throw wrapException(e, false);
		}
	}

	public void loadAndCallTests(boolean passDirectory) {

		try {
			logs("Indexing test() function");
			logn(DONE);
			stdout.print("\n");
			ScriptValue testFunction = globals.get(globals.getValueFactory().translate("test", globals));
			stdout.print("\n");

			logs("Calling test() function");
			logn(STARTED);
			stdout.print("\n");

			ScriptValue result;
			if (passDirectory)
				result = testFunction.getAsFunction().call(
						globals.getValueFactory().translate(System.getProperty("user.dir"), globals)
				);
			else
				result = testFunction.call();

			stdout.print("\n");

			if (result.canTranslateInt()) {
				int i = result.translateInt();
				if (i != 0) {
					throw new RuntimeException("J: test() return value: " + i);
				}
				stdout.println("J: test() return value: " + i);
			} else {
				throw new RuntimeException("non-integral response returned from test() lua function");
			}
		}
		catch (ScriptError e) {
			throw wrapException(e, false);
		}
	}

	public void dumpValues() {
		long s = LuaNEngine.ENGINE_INTERFACE.contextsize();
		ScriptValue[] arr = new ScriptValue[(int) s];

		for (long t = 0; t < s && t < arr.length; t++) {
			arr[(int) t] = LuaNEngine.ENGINE_INTERFACE.getvalue(t);
		}

		String dump = Arrays.asList(arr).stream()
				.map(this::valueString)
				.collect(Collectors.joining(", "));

		stdout.println("Remaining values:");
		stdout.println(dump);
	}

	public void cleanupThreadContext() {
		globals.cleanupThreadContext();
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
		/*
		if (debuggerProcess != null && debuggerProcess.isAlive()) {
			debuggerProcess.destroyForcibly();
			debuggerProcess.waitFor();
		}
		*/
		// give time for the previous GDB instance to exit
		if (DEBUG) {
			Thread.sleep(1000);
		}
	}

	public class Foo {
		public int foo() {
			stdout.println("J: Foo#foo() called!");
			return 42;
		}
		public String bar(int x, int y) {
			stdout.println("J: Foo#bar(int, int) called!");
			return "0x" + Integer.toHexString(x + y).toUpperCase();
		}
	}

	public String[] $stringArray() {
		return new String[] { "foo", "biz", "bar", "woof" };
	}

	public Foo[] $objectArray() {
		return new Foo[] { new Foo(), new Foo() };
	}

	public int $testIntReturn() {
		return 42;
	}

	public String $testStringReturn() {
		return "foobar";
	}

	public Foo $newFooObject() {
		return new Foo();
	}

	public String $testFunction(int arg0, String arg1) {
		stdout.println("J: testFunction sucessfully called from Lua!");
		return "ret: ('" + arg1 + "', " + arg0 + ")";
	}

	public void $setdebug(boolean debug) {
		Computers.debug = debug;
	}

	public void $throwSomething() {
		throw new IllegalStateException("foo");
	}

	public void $submitCallback(FunctionBind bind) {
		assert (Double) bind.call(2, 6) == 8;
	}

	public void $submitPartialCallback(PartialFunctionBind bind) {
		assert bind.call(5, 3).translateInt() == 8;
	}

	public void $submitValueCallback(ScriptValue value) {
		ScriptValue four = globals.getValueFactory().translate(4, globals);
		assert value.getAsFunction().call(four, four).translateInt() == 8;
	}

	public void logs(String message) {
		stdout.println(SPLITTER);
		log(message);
	}

	public void log(String message) {
		stdout.print("| " + message);
		stdout.flush();
		loglen += message.length();
	}

	public void logn(String message) {
		logi(message);
		stdout.println(SPLITTER);
	}

	public void logi(String message) {
		StringBuilder builder = new StringBuilder();
		builder.append(" ");
		for (int t = 0; t < LOG_TAB - (loglen + 2) - (message.length() - 6); t++)
			builder.append(".");
		builder.append(" ");
		stdout.print(builder.toString());
		stdout.println(message + " |");
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
			logi(DONE);
			return process;
		} catch (IOException e) {
			throw new RuntimeException("Unable to run debug hook command", e);
		}
	}
}
