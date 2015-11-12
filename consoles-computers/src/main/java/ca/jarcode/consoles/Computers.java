package ca.jarcode.consoles;

import ca.jarcode.consoles.api.Position2D;
import ca.jarcode.consoles.computer.ComputerHandler;
import ca.jarcode.consoles.computer.GeneralListener;
import ca.jarcode.consoles.computer.MapDataStore;
import ca.jarcode.consoles.computer.NativeLoader;
import ca.jarcode.consoles.computer.command.CommandComputer;
import ca.jarcode.consoles.computer.interpreter.Lua;
import ca.jarcode.consoles.computer.interpreter.LuaDefaults;
import ca.jarcode.consoles.computer.interpreter.luaj.LuaJEngine;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNEngine;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNImpl;
import ca.jarcode.consoles.internal.ConsoleHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class Computers extends JavaPlugin {

	private static Computers INSTANCE = null;

	// flag we use in case the plugin is reloaded, the JVM complains if we try to re-load the same native
	private static boolean LOADED_NATIVES = false;
	private static boolean ATTEMPTED_NATIVES_LOAD = false;

	private static String NATIVE_IMPL = "computerimpl";

	// allow the crafting of computers
	public static boolean allowCrafting = true;
	// whether to enable the basic frame rendering for computers
	public static boolean frameRenderingEnabled = true;
	// command prefix
	public static String commandPrefix;
	// self-explanatory
	public static int maxComputers = 3;
	// whether to use player heads as items for computers
	public static boolean useHeads = true;
	// head to use for computer items
	public static String headUUID = "df045cc0-7ec9-4cbc-8219-2ea32ee0cd84";
	public static String headTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQxZTk5NzkyODlmMDMwOTlhN2M1ODdkNTJkNDg4ZTI2ZTdiYjE3YWI1OTRiNjlmOTI0MzhkNzdlYWJjIn19fQ==";
	// hide save messages
	public static boolean hideSaveMessages = false;
	// maximum time that a program is allowed to run before being interrupted
	public static int maxTimeWithoutInterrupt = 7000;
	// chunk size (in kilobytes) that wget downloads every 300ms
	public static int wgetChunkSize = 2;
	// max heap size for scripts (in kilobytes)
	public static int scriptHeapSize = 64 * 1024;
	// script interpreter to use
	public static String scriptEngine = "luaj";
	// amount of instructions to wait before checking
	public static int interruptCheckInterval = 200;
	// debug mode
	public static boolean debug = false;
	// debug hook
	public static boolean debugHook = false;
	// debug hook command
	public static String debugHookCommand = "x-terminal-emulator,-e,gdb,-p,%I,-ex,catch syscall exit exit_group,-ex,cont";

	public static File jarFile;

	public static Computers getInstance() {
		return INSTANCE;
	}

	{
		INSTANCE = this;
	}

	// Be careful if you touch this onEnable method, certain classes are not allowed to be referenced within
	// the lifetime of the VM if natives weren't able to be loaded properly. This is to maintain support
	// for OSX and Windows using the LuaJ interpreter.

	// Reloading the plugin is a difficult task with natives (the whole concept of 'reloading' plugins
	// when everything is garbage collected _is_ inheritly stupid too), since native libraries are
	// actually closed when the plugin's classloader is GC'd.

	// We're going to _assume_ that if this class (not instance) is GC'd, then its classloader has also
	// been collected and that we'll need to re-load our native component. This is not a gaurantee, but
	// it works 99% of the time.

	// bukkit's /reload command should work fine with this plugin since it technnically doesn't attempt
	// to unload anything, but there is a possibility that shit could hit the fan if a separate plugin
	// actually tries to unload this.

	public void onEnable() {

		HashMap<String, Runnable> engines = new HashMap<>();

		jarFile = getFile();

		Lua.killAll = false; // if this plugin was reloaded
		saveDefaultConfig();

		frameRenderingEnabled = getConfig().getBoolean("frame-rendering", frameRenderingEnabled);
		allowCrafting = getConfig().getBoolean("allow-computer-crafting", allowCrafting);
		commandPrefix = getConfig().getString("command-prefix", commandPrefix).trim();
		maxComputers = getConfig().getInt("computer-limit", maxComputers);
		useHeads = getConfig().getBoolean("use-heads", useHeads);
		headUUID = getConfig().getString("head-uuid", headUUID);
		headTexture = getConfig().getString("head-texture", headTexture);
		hideSaveMessages = getConfig().getBoolean("hide-save-messages", hideSaveMessages);
		maxTimeWithoutInterrupt = getConfig().getInt("max-time-without-interrupt", maxTimeWithoutInterrupt);
		wgetChunkSize = getConfig().getInt("wget-chunk-size", wgetChunkSize);
		scriptHeapSize = getConfig().getInt("script-heap-size", scriptHeapSize);
		scriptEngine = getConfig().getString("script-engine", scriptEngine).toLowerCase();
		interruptCheckInterval = getConfig().getInt("interrupt-check-interval", interruptCheckInterval);
		debug = getConfig().getBoolean("debug-mode", debug);
		debugHook = getConfig().getBoolean("debug-hook", debugHook);
		debugHookCommand = getConfig().getString("debug-command", debugHookCommand);

		boolean disableWatchdog = getConfig().getBoolean("disable-watchdog", false);

		loadAttempt: if (!LOADED_NATIVES && !ATTEMPTED_NATIVES_LOAD) {
			ATTEMPTED_NATIVES_LOAD = true;
			// extract JNI library and load it
			if (!new NativeLoader(NATIVE_IMPL).loadAsJNILibrary(this))
				break loadAttempt;

			LOADED_NATIVES = true;
		}

		if (!LOADED_NATIVES) {
			getLogger().warning("");
			getLogger().warning("Support for the LuaJIT and standard Lua interpreters " +
							"is unavailable on your environment (" + System.getProperty("os.name") +
							" " + System.getProperty("os.arch") + ")"
			);
			getLogger().warning("");
			if (System.getProperty("os.name").toLowerCase().contains("linux")) {
				getLogger().warning("Installing luajit and libffi (also called 'libffi6') " +
						"and restarting your server should resolve the issue.");
				getLogger().warning("On Ubuntu/Debian, you can run: ");
				getLogger().warning("");
				getLogger().warning("\tapt-get install luajit libffi6");
				getLogger().warning("");
			}
		}
		if (debugHook) {
			if (!LOADED_NATIVES) {
				getLogger().warning("Skipping debug hook, natives were never loaded.");
			}
			else {
				attachDebugger();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
					getLogger().warning("Failed to wait for debug hook");
				}
			}
		}

		engines.put("luaj", LuaJEngine::install);

		if (LOADED_NATIVES) {
			engines.put("lua", () -> LuaNEngine.install(LuaNImpl.DEFAULT));
			engines.put("luajit", () -> LuaNEngine.install(LuaNImpl.JIT));
		}

		if (!engines.containsKey(scriptEngine)) {
			scriptEngine = "luaj";
		}

		getLogger().info("Using script engine: " + scriptEngine);

		engines.get(scriptEngine).run();

		MapDataStore.init(this);

		register(ComputerHandler::new, GeneralListener::new);

		try {
			Consoles consoles = Consoles.getInstance();
			consoles.getCommandHandler().addCommand(CommandComputer.class);
			ConsoleHandler.getInstance().interactionHooks.add((x, y, player, console) -> {
				ComputerHandler handler = ComputerHandler.getInstance();
				if (handler != null) {
					handler.interact(new Position2D(x, y), player, console);
				}
			});
		}
		catch (NoClassDefFoundError e) {
			getLogger().warning("This shouldn't happen!");
			e.printStackTrace();
		}
		catch (IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}

		if (Computers.debug)
			LuaDefaults.cacheTests();

		if (disableWatchdog) {
			getServer().getScheduler().scheduleSyncDelayedTask(this, this::killWatchdog);
		}
	}

	// This kills the watchdog thread. Thankfully there is a nice method for it.
	// Reflection is used so that we don't have to worry about breaking this plugin for
	// people using pure craftbukkit builds.
	@SuppressWarnings("unchecked")
	public void killWatchdog() {
		try {
			Class type = Class.forName("org.spigotmc.WatchdogThread");
			Method method = type.getMethod("doStop");
			method.invoke(null);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void onDisable() {
		Lua.killAll = true;
	}

	private void register(Supplier... suppliers) {
		for (Supplier supplier : suppliers) {
			Object obj = supplier.get();
			if (obj instanceof Listener)
				this.getServer().getPluginManager().registerEvents((Listener) obj, this);
		}
	}
	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void attachDebugger() {
		String beanName = ManagementFactory.getRuntimeMXBean().getName();
		if (!beanName.contains("@")) {
			getLogger().severe("Unable to find process ID to start debug hook");
			return;
		}
		String sub = beanName.split("@")[0];
		try {
			Integer.parseInt(sub);
		}
		catch (NumberFormatException e) {
			getLogger().severe("Unable to parse MX bean to find process ID");
			return;
		}
		String command = debugHookCommand;

		Function<String, String> format = NativeLoader.libraryFormat(NativeLoader.getNativeTarget());

		command = command.replace("%I", sub);
		command = command.replace("%N", "debug-" + sub);
		command = command.replace("%E", new File(getDataFolder(), format.apply(NATIVE_IMPL)).getAbsolutePath());

		try {
			getLogger().info(String.format("Starting debug hook (command %s)", command));
			new ProcessBuilder()
					.command(command.split(","))
					.directory(getDataFolder())
					.start();
			getLogger().info("Hook started...");
		} catch (IOException e) {
			e.printStackTrace();
			getLogger().severe("Unable to run debug hook command");
		}
	}
}
