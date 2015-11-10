
import ca.jarcode.consoles.computer.NativeLoader;
import ca.jarcode.consoles.computer.interpreter.FuncPool;
import ca.jarcode.consoles.computer.interpreter.Lua;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptEngine;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;
import ca.jarcode.consoles.computer.interpreter.interfaces.ValueFactory;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNEngine;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNError;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNImpl;

import org.junit.After;
import org.junit.Test;

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
public class NativeLayerTest {

	public static boolean DEBUG = false;

	public static String GDB_CMD = "x-terminal-emulator,-e,gdb,-p,%I";

	private static final int LOG_TAB = 69;

	private static final String DONE = "[DONE]";

	// getting original stdout, don't want to use JUnit's crap
	OutputStream out = new FileOutputStream(FileDescriptor.out);
	PrintStream stdout = new PrintStream(out);

	Process debuggerProcess;

	FuncPool pool;
	ScriptValue globals;

	public int loglen = 0;

	@Test
	public void testNativeLayer() throws Throwable {

		stdout.print("\n");

		log("Loading tests and setting up engine");

		File library = new File(
				System.getProperty("user.dir") + File.separator +
				"target/natives" + File.separator +
				NativeLoader.libraryFormat(NativeLoader.getNativeTarget()).apply("computerimpl")
		);

		assert library.exists();
		assert library.isFile();

		System.load(library.getAbsolutePath());

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

		LuaNEngine.install(LuaNImpl.JIT);

		// map a lambda vesion of our test function
		Lua.map(this::lua$testFunction, "lambdaTestFunction");

		pool = new FuncPool(null);

		// register the current thread
		pool.register(Thread.currentThread());

		// register any lua functions
		Lua.find(this, pool);

		logn(DONE);

		if (DEBUG) {
			debuggerProcess = attachDebugger(library.getAbsolutePath());
			Thread.sleep(1000);
		}

		log("Building new engine instance");
		globals = ScriptEngine.get().newInstance(pool, null, System.in, System.out, -1);
		logn(DONE);

		log("Loading program chunk");
		ScriptValue chunk = ScriptEngine.get().load(globals, program);
		logn(DONE);

		log("Calling program chunk");
		chunk.call();
		logn(DONE);

		log("Indexing test() function");
		ScriptValue testFunction = chunk.get(ValueFactory.get().translate("test", globals));
		logn(DONE);

		log("Calling test() function");
		ScriptValue result = testFunction.getAsFunction().call();
		logn(DONE);

		log("\n");

		if (result.canTranslateInt()) {
			int i = result.translateInt();
			if (i != 0) {
				throw new RuntimeException("test() return value: " + i);
			}
			logn("test() return value: " + i);
		}
		else {
			throw new RuntimeException("non-integral response returned from test() lua function");
		}

		// these should be cleaned up anyway, but if releasing doesn't work for some reason, we
		// should call these anyway.
		chunk.release();
		testFunction.release();
		result.release();

		if (DEBUG) {
			debuggerProcess.destroyForcibly();
			debuggerProcess.waitFor();
		}
	}

	@After
	public void cleanup() throws InterruptedException {
		if (globals != null) {
			ScriptEngine.get().close(globals);
		}
		if (pool != null) {
			pool.cleanup();
		}
		if (debuggerProcess != null && debuggerProcess.isAlive()) {
			debuggerProcess.destroyForcibly();
			debuggerProcess.waitFor();
		}
	}

	public String lua$testFunction(int arg0, String arg1) {
		return "ret: ('" + arg1 + "', " + arg0 + ")";
	}

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
