package ca.jarcode.consoles;

import ca.jarcode.consoles.api.Position2D;
import ca.jarcode.consoles.computer.ComputerHandler;
import ca.jarcode.consoles.computer.GeneralListener;
import ca.jarcode.consoles.computer.MapDataStore;
import ca.jarcode.consoles.computer.NativeLoader;
import ca.jarcode.consoles.computer.command.CommandComputer;
import ca.jarcode.consoles.computer.interpreter.Lua;
import ca.jarcode.consoles.internal.ConsoleHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.function.Supplier;

public class Computers extends JavaPlugin {

	private static Computers INSTANCE = null;

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

	public static File jarFile;

	public static Computers getInstance() {
		return INSTANCE;
	}

	{
		INSTANCE = this;
	}

	public void onEnable() {

		new NativeLoader("computerimpl").loadAsJNILibrary(this);

		jarFile = getFile();

		Lua.killAll = false; // if this plugin was reloaded
		saveDefaultConfig();

		frameRenderingEnabled = getConfig().getBoolean("frame-rendering", true);
		allowCrafting = getConfig().getBoolean("allow-computer-crafting", true);
		commandPrefix = getConfig().getString("command-prefix", "/").trim();
		maxComputers = getConfig().getInt("computer-limit", 3);
		hideSaveMessages = getConfig().getBoolean("hide-save-messages", false);
		maxTimeWithoutInterrupt = getConfig().getInt("max-time-without-interrupt", 7000);
		wgetChunkSize = getConfig().getInt("wget-chunk-size", 2);

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
