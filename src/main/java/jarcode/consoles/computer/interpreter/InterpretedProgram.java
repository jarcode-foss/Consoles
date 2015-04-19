package jarcode.consoles.computer.interpreter;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Program;
import jarcode.consoles.computer.ProgramInstance;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.bin.MakeDirectoryProgram;
import jarcode.consoles.computer.bin.TouchProgram;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.interpreter.types.*;
import org.bukkit.ChatColor;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

	private List<Integer> allocatedSessions = new ArrayList<>();
	public Map<Integer, LuaFrame> framePool = new HashMap<>();

	public InterpretedProgram(FSFile file) {
		this.file = file;
	}
	@SuppressWarnings("SpellCheckingInspection")
	private void map() {

		Lua.put(this::lua_args, "args", pool);
		Lua.put(this::lua_clear, "clear", pool);
		Lua.put(this::lua_print, "printc", pool);
		Lua.put(this::lua_read, "read", pool);
		Lua.put(this::lua_resolveFile, "resolveFile", pool);
		Lua.put(this::lua_resolveFolder, "resolveFolder", pool);
		Lua.put(this::lua_touch, "touch", pool);
		Lua.put(this::lua_mkdir, "mkdir", pool);
		Lua.put(this::lua_reflect, "reflect", pool);
		Lua.put(this::lua_write, "write", pool);
		Lua.put(this::lua_sleep, "sleep", pool);

		if (Consoles.componentRenderingEnabled) {
			Lua.put(this::lua_registerPainter, "paint", pool);
		}

		if (Consoles.frameRenderingEnabled) {
			Lua.put(this::lua_screenBuffer, "screenBuffer", pool);
			Lua.put(this::lua_screenFrame, "screenFrame", pool);
		}
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
			globals.load(new InterruptLib(this::terminated));
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
			// we handle errors with exceptions
			globals.STDERR = new PrintStream(out) {
				@Override
				public void println(String x) {}

				@Override
				public void println(Object x) {}
			};
			globals.STDIN = in;
			LuaValue chunk;
			LuaValue exit = null;
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
				LuaValue value = globals.get("main");
				exit = globals.get("exit");
				if (value.isfunction()) {
					value.call(args);
				}
			}
			catch (ProgramInterruptException ex) {
				print("\nProgram terminated");
			}
			catch (LuaError err) {
				handleLuaError(err);
			}
			finally {
				if (exit != null && exit.isfunction() && !terminated()) {
					try {
						exit.call();
					}
					catch (ProgramInterruptException ex) {
						print("\nExit routine terminated");
					}
					catch (LuaError err) {
						handleLuaError(err);
					}
				}
			}
		}
		finally {
			if (pool != null)
				pool.cleanup();
			framePool.clear();
			Terminal terminal = computer.getTerminal(this);
			terminal.setHandlerInterrupt(null);
			for (int i : allocatedSessions) {
				computer.setComponent(i, null);
			}
		}
	}
	public Computer getComputer() {
		return computer;
	}
	private void handleLuaError(LuaError err) {
		if (Consoles.DEBUG)
			err.printStackTrace();
		println("RUNTIME ERROR:");
		String msg = Arrays.asList(err.getMessage().split("\n")).stream()
				.map(this::err)
				.collect(Collectors.joining("\n"));
		print(msg);
	}
	protected String warning(String str) {
		return "\t" + ChatColor.YELLOW + str;
	}
	protected String err(String str) {
		return "\t" + ChatColor.RED + str;
	}
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	protected String lua_read() {
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
	private LuaBuffer lua_screenBuffer(Integer index) {
		index--;
		if (!computer.screenAvailable(index)) return null;
		BufferedFrameComponent component = new BufferedFrameComponent(computer);
		return new LuaBuffer(this, index, component);
	}
	private void lua_write(String text) {
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
	protected String lua_args() {
		System.out.print("called");
		return args;
	}
	private LuaFolder lua_resolveFolder(String path) {
		FSBlock block = resolve(path);
		return block instanceof FSFolder ? new LuaFolder((FSFolder) block, path,
				computer.getTerminal(this).getCurrentDirectory(), this::terminated, computer) : null;
	}
	private LuaFile lua_resolveFile(String path) {
		FSBlock block = resolve(path);
		return block instanceof FSFile ? new LuaFile((FSFile) block, path,
				computer.getTerminal(this).getCurrentDirectory(), this::terminated, computer) : null;
	}
	private LuaFrame lua_screenFrame() {
		int id = findFrameId();
		LuaFrame frame = new LuaFrame(id, computer);
		framePool.put(id, frame);
		return frame;
	}
	private int findFrameId() {
		int i = 0;
		while(framePool.containsKey(i))
			i++;
		return i;
	}
	private FSFile lua_touch(String path) {
		return new TouchProgram(false).touch(path, computer, computer.getTerminal(this));
	}
	public String[] lua_reflect() {
		return pool.functions.keySet().toArray(new String[pool.functions.size()]);
	}
	public void lua_sleep(Integer ms) {
		try {
			long target = System.currentTimeMillis() + ms;
			while (System.currentTimeMillis() < target && !terminated()) {
				Thread.sleep(8);
			}
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	@SuppressWarnings("SpellCheckingInspection")
	private FSFolder lua_mkdir(String path) {
		return new MakeDirectoryProgram(false).mkdir(path, computer, computer.getTerminal(this));
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
