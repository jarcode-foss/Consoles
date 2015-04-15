package jarcode.consoles.computer.interpreter;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Program;
import jarcode.consoles.computer.ProgramInstance;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import org.bukkit.ChatColor;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class InterpretedProgram implements Program {

	private FSFile file;
	private InputStream in;
	private OutputStream out;
	private Computer computer;
	private Charset charset = Charset.forName("UTF-8");
	private ProgramInstance instance;
	private FuncPool pool;
	private String args;

	public InterpretedProgram(FSFile file) {
		this.file = file;
	}
	@SuppressWarnings("SpellCheckingInspection")
	private void map() {
		Lua.put(this::lua_args, "args", pool);
		Lua.put(this::lua_clear, "clear", pool);
		Lua.put(this::lua_print, "printc", pool);
	}
	public void run(OutputStream out, InputStream in, String str, Computer computer, ProgramInstance instance) throws Exception {
		try {
			this.in = in;
			this.out = out;
			this.computer = computer;
			this.instance = instance;
			this.args = str;
			pool = new FuncPool(Thread.currentThread(), computer);
			map();
			if (file == null) {
				print("null file");
				return;
			}
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
			String raw = new String(buf.toByteArray(), charset);
			Globals globals = new Globals();
			globals.load(new JseBaseLib());
			globals.load(new PackageLib());
			globals.load(new Bit32Lib());
			globals.load(new TableLib());
			globals.load(new StringLib());
			globals.load(new JseMathLib());
			globals.load(new GameIoLib(instance));
			globals.load(new InterruptLib());
			globals.load(new MathLib());
			LoadState.install(globals);
			LuaC.install(globals);

			globals.load(new BaseLib());

			for (Map.Entry<String, LibFunction> entry : pool.functions.entrySet()) {
				if (Consoles.DEBUG)
					System.out.println("mapping: " + entry.getKey() + ", to " + entry.getValue());
				globals.set(entry.getKey(), entry.getValue());
			}
			globals.STDOUT = new PrintStream(out);
			globals.STDERR = new PrintStream(out) {
				@Override
				public void println(String x) {
					super.println(ChatColor.RED.toString() + x);
				}

				@Override
				public void println(Object x) {
					super.println(ChatColor.RED.toString() + x.toString());
				}
			};
			globals.STDIN = in;
			LuaValue chunk;
			try {
				chunk = globals.load(raw);
			} catch (LuaError err) {
				if (Consoles.DEBUG)
					err.printStackTrace();
				println("COMPILE ERROR:");
				String msg = Arrays.asList(err.getMessage().split("\n")).stream()
						.map(this::warning)
						.collect(Collectors.joining("\n"));
				print(msg);
				return;
			}
			try {
				chunk.call();
			} catch (LuaError err) {
				if (Consoles.DEBUG)
					err.printStackTrace();
				println("RUNTIME ERROR:");
				String msg = Arrays.asList(err.getMessage().split("\n")).stream()
						.map(this::err)
						.collect(Collectors.joining("\n"));
				print(msg);
			}
		}
		finally {
			if (pool != null)
				pool.cleanup();
		}
	}
	protected String warning(String str) {
		return "\t" + ChatColor.YELLOW + str;
	}
	protected String err(String str) {
		return "\t" + ChatColor.RED + str;
	}
	protected String lua_args() {
		System.out.print("called");
		return args;
	}
	protected void lua_print(String formatted) {
		formatted = ChatColor.translateAlternateColorCodes('&', formatted);
		println(formatted);
	}
	protected void print(String formatted) {
		try {
			out.write(formatted.getBytes(charset));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected void println(String formatted) {
		print(formatted + '\n');
	}
	protected void nextln() {
		print("\n");
	}
	protected FSBlock resolve(String input) {
		return computer.resolve(input, this);
	}
	protected boolean terminated() {
		return instance.isTerminated();
	}
	public Boolean lua_clear() {
		try {
			// this is to prevent spam & waiting for the last thing in the buffer to write
			Thread.sleep(50);
			return schedule(() -> {
				Terminal terminal = computer.getTerminal(this);
				if (terminal != null)
					terminal.clear();
				return true;
			});
		}
		catch (InterruptedException e) {
			if (Consoles.DEBUG)
				e.printStackTrace();
			return false;
		}
	}
}
