package jarcode.consoles.computer.interpreter;

import com.google.common.base.Joiner;
import jarcode.consoles.Consoles;
import jarcode.consoles.computer.*;
import jarcode.consoles.computer.bin.MakeDirectoryProgram;
import jarcode.consoles.computer.bin.TouchProgram;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.interpreter.types.*;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class InterpretedProgram implements Program {

	public Map<Integer, LuaFrame> framePool = new HashMap<>();
	
	private FSFile file;
	private String path;
	private InputStream in;
	private OutputStream out;
	private Computer computer;
	private Charset charset = Charset.forName("UTF-8");
	private ProgramInstance instance;
	private FuncPool pool;
	private String args;
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private List<Integer> allocatedSessions = new ArrayList<>();
	private Globals globals;

	private InterruptLib interruptLib = new InterruptLib(this::terminated);

	private List<String> registeredChannels = new ArrayList<>();

	public InterpretedProgram(FSFile file, String path) {
		this.file = file;
		this.path = path;
	}
	@SuppressWarnings("SpellCheckingInspection")
	private void map() {

		// instance lua functions, these will call with the program context in mind
		// maps all functions that start with lua$
		Lua.find(this, pool);

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
			globals = new Globals();
			globals.load(new JseBaseLib());
			globals.load(new PackageLib());
			globals.load(new Bit32Lib());
			globals.load(new TableLib());
			globals.load(new StringLib());
			globals.load(new EmbeddedMathLib());
			globals.load(interruptLib);
			Lua.libraries.values().forEach(globals::load);
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
				println("lua:" + ChatColor.RED + " compile error");
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
			registeredChannels.forEach(computer::unregisterMessageListener);
			registeredChannels.clear();
			Terminal terminal = computer.getTerminal(this);
			if (terminal != null) {
				terminal.setHandlerInterrupt(null);
				terminal.setIgnoreUnauthorizedSigterm(false);
			}
			for (int i : allocatedSessions) {
				computer.setComponent(i, null);
			}
		}
	}
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
		if (Consoles.DEBUG)
			err.printStackTrace();
		println("lua:" + ChatColor.RED + " runtime error");
		String[] arr = Arrays.asList(err.getMessage().split("\n")).stream()
				.map(this::err)
				.toArray(String[]::new);
		String msg = Joiner.on('\n').join(arr);
		if (arr.length > 16) {
			Terminal terminal = computer.getTerminal(this);
			if (terminal != null) {
				LuaFile file;
				int i = 0;
				while (resolve("lua_dump" + i) != null) {
					i++;
				}
				file = lua$touch("lua_dump" + i);
				assert file != null;
				String version;
				try {
					version = globals.get("_VERSION").checkjstring();
				}
				catch (LuaError ignored) {
					version = "?";
				}
				msg = "Lua stack trace from " + format.format(new Date(System.currentTimeMillis())) + "\n"
						+ "Lua version: " + version + "\n\n"
						+ ChatColor.stripColor(msg.replace("\t", "    "));
				file.write(msg);
				String cd = terminal.getCurrentDirectory();
				if (cd.endsWith("/"))
					cd = cd.substring(0, cd.length() - 1);
				println("lua:" + ChatColor.RED + " stack trace too large!");
				print("lua:" + ChatColor.RED + " dumped: " + ChatColor.YELLOW + cd + "/" + "lua_dump" + i);
			}
		}
		else
			print(msg);
	}
	private String warning(String str) {
		return "\t" + ChatColor.YELLOW + str;
	}
	private String err(String str) {
		return "\t" + ChatColor.RED + str;
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
	private String lua$hostname() {
		return computer.getHostname();
	}
	private LuaComputer lua$findComputer(String hostname) {
		Computer computer = ComputerHandler.getInstance().find(hostname);
		if (computer == null) return null;
		else return new LuaComputer(computer);
	}
	private LuaChannel lua$registerChannel(String channel) {
		if (!computer.isChannelRegistered(channel)) {
			LuaChannel ch = new LuaChannel(interruptLib::update, () -> {
				computer.unregisterMessageListener(channel);
				registeredChannels.remove(channel);
			});
			computer.registerMessageListener(channel, ch::append);
			registeredChannels.add(channel);
			return ch;
		}
		else return null;
	}
	private void lua$ignoreTerminate(Boolean ignore) {
		getComputer().getTerminal(this).setIgnoreUnauthorizedSigterm(ignore);
	}
	private String[] lua$soundList() {
		return Arrays.asList(Sound.values()).stream()
				.map(Enum::name)
				.toArray(String[]::new);
	}
	private void lua$sound(String name, LuaValue v1, LuaValue v2) {
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
	private String lua$programDir() {
		String[] arr = this.path.split("/");
		return Arrays.asList(arr).stream()
				.limit(arr.length - 1)
				.collect(Collectors.joining("/"));
	}
	private String lua$programPath() {
		return path;
	}
	private LuaValue lua$require(String path) {
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
			if (Consoles.DEBUG)
				err.printStackTrace();
			println("lua:" + ChatColor.RED + " failed to compile '" + path + "'");
			String msg = Arrays.asList(err.getMessage().split("\n")).stream()
					.map(this::warning)
					.collect(Collectors.joining("\n"));
			println(msg);
			return null;
		}
		return value.call();
	}
	private LuaBuffer lua_screenBuffer(Integer index) {
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
		System.out.print("called");
		return args;
	}
	private LuaFolder lua$resolveFolder(String path) {
		FSBlock block = resolve(path);
		return block instanceof FSFolder ? new LuaFolder((FSFolder) block, path,
				computer.getTerminal(this).getCurrentDirectory(), this::terminated, computer) : null;
	}
	private LuaFile lua$resolveFile(String path) {
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
	private int lua$chestList() {
		return ComputerHandler.getInstance().findChests(computer).length;
	}
	private LuaValue lua$getChest(int index) {
		Chest[] chests = ComputerHandler.getInstance().findChests(computer);
		if (index > chests.length || index < 0) return LuaValue.NIL;
		LuaChest lua = new LuaChest(chests[index]);
		return CoerceJavaToLua.coerce(lua);
	}
	private LuaFile lua$touch(String path) {
		FSFile file = new TouchProgram(false).touch(path, computer, computer.getTerminal(this));
		return file != null ? new LuaFile(file, path,
				computer.getTerminal(this).getCurrentDirectory(), this::terminated, computer) : null;
	}
	private String[] lua$reflect() {
		return pool.functions.keySet().toArray(new String[pool.functions.size()]);
	}
	private void lua$sleep(Integer ms) {
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
		FSFolder folder = new MakeDirectoryProgram(false).mkdir(path, computer, computer.getTerminal(this));
		return folder != null ? new LuaFolder(folder, path,
				computer.getTerminal(this).getCurrentDirectory(), this::terminated, computer) : null;
	}
	@SuppressWarnings("SpellCheckingInspection")
	private void lua$printc(String formatted) {
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
	protected void nextL() {
		print("\n");
	}
	protected FSBlock resolve(String input) {
		return computer.resolve(input, this);
	}
	protected boolean terminated() {
		return instance.isTerminated();
	}
}
