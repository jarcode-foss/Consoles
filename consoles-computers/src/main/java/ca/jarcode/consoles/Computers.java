package ca.jarcode.consoles;

import ca.jarcode.consoles.api.Position2D;
import ca.jarcode.consoles.computer.ComputerHandler;
import ca.jarcode.consoles.computer.GeneralListener;
import ca.jarcode.consoles.computer.MapDataStore;
import ca.jarcode.consoles.computer.NativeLoader;
import ca.jarcode.consoles.computer.command.CommandComputer;
import ca.jarcode.consoles.computer.interpreter.Lua;
import ca.jarcode.consoles.computer.interpreter.luaj.LuaJEngine;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNEngine;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNImpl;
import ca.jarcode.consoles.internal.ConsoleHandler;
import jni.LibLoader;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.function.Supplier;

public class Computers extends JavaPlugin {

	private static Computers INSTANCE = null;

	// flag we use in case the plugin is reloaded, the JVM complains if we try to re-load the same native
	private static boolean LOADED_NATIVES = false;
	private static boolean ATTEMPTED_NATIVES_LOAD = false;

	// allow the crafting of computers
	public static boolean allowCrafting = true;
	// whether to enable the basic frame rendering for computers
	public static boolean frameRenderingEnabled = true;
	// command prefix
	public static String commandPrefix;
	// self-explanatory
	public static int maxComputers = 3;
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
		hideSaveMessages = getConfig().getBoolean("hide-save-messages", hideSaveMessages);
		maxTimeWithoutInterrupt = getConfig().getInt("max-time-without-interrupt", maxTimeWithoutInterrupt);
		wgetChunkSize = getConfig().getInt("wget-chunk-size", wgetChunkSize);
		scriptHeapSize = getConfig().getInt("script-heap-size", scriptHeapSize);
		scriptEngine = getConfig().getString("script-engine", scriptEngine).toLowerCase();
		interruptCheckInterval = getConfig().getInt("interrupt-check-interval", interruptCheckInterval);

		loadAttempt: if (!LOADED_NATIVES && !ATTEMPTED_NATIVES_LOAD) {
			ATTEMPTED_NATIVES_LOAD = true;
			// extract JNI library and load it
			if (!new NativeLoader("computerimpl").loadAsJNILibrary(this))
				break loadAttempt;
			// link an instance of `LibLoader` so that we can use dlopen/dlclose within Java
			NativeLoader.linkLoader(new LibLoader());

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
}
