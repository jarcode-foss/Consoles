package ca.jarcode.consoles;

import ca.jarcode.classloading.loader.WrappedPlugin;
import ca.jarcode.consoles.api.nms.ConsolesNMS;
import ca.jarcode.consoles.images.ImageConsoleHandler;
import ca.jarcode.consoles.internal.ConsoleHandler;
import ca.jarcode.consoles.messaging.ConsoleBungeeHook;
import ca.jarcode.consoles.command.*;
import ca.jarcode.consoles.util.MapInjector;
import ca.jarcode.consoles.util.sync.SyncTaskScheduler;
import org.bukkit.event.Listener;

import java.util.function.Supplier;

/*

The actual Consoles plugin. Nothing much here, just registration for
events, static stuff, and configuration reading.

 */
public class Consoles extends WrappedPlugin {

	private static Consoles instance;

	// starting index for map ID allocations
	public static short startingId;
	// debug mode
	public static boolean debug = false;

	static {
		if (!Consoles.class.getClassLoader().getClass().getSimpleName().equals("WrappedClassLoader")) {
			Thread.dumpStack();
			System.exit(0);
			throw new RuntimeException();
		}
	}

	public static Consoles getInstance() {
		return instance;
	}

	{
		instance = this;

		NMSHandler.init();
	}

	private CommandHandler commandHandler;

	@Override
	public void onEnable() {
		saveDefaultConfig();

		boolean forward = getConfig().getBoolean("bungee-forward", false);

		startingId = (short) getConfig().getInt("starting-map-index", 5000);
		debug = getConfig().getBoolean("debug-mode", false);

		ConsoleHandler.getInstance().local = !forward;

		ConsolesNMS.mapInternals.injectTypes();
		ConsolesNMS.mapInternals.clearVanillaMapFiles();

		commandHandler = new CommandHandler(
				CommandConsole.class, CommandImage.class
		);

		register(
				ConsoleHandler::getInstance, ConsoleBungeeHook::new, SyncTaskScheduler::create,
				this::getCommandHandler, ImageConsoleHandler::new
		);

		ImageConsoleHandler imageHandler = new ImageConsoleHandler();
		getServer().getScheduler().scheduleSyncDelayedTask(this, imageHandler::load);
	}

	@Override
	public void onDisable() {
		ConsoleHandler.getInstance().getPainter().stop();
		try {
			SyncTaskScheduler.getInstance().end();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// this is made just because I like using :: and ->
	// no, you can't just pass references to the listeners.
	// that would be boring.
	private void register(Supplier... suppliers) {
		for (Supplier supplier : suppliers) {
			Object obj = supplier.get();
			if (obj instanceof Listener)
				this.getServer().getPluginManager().registerEvents((Listener) obj, this);
		}
	}
	public CommandHandler getCommandHandler() {
		return commandHandler;
	}
	public void registerCommand(Class<? extends CommandBase> base) throws InstantiationException, IllegalAccessException {
		commandHandler.addCommand(base);
	}
}
