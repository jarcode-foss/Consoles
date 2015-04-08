package jarcode.consoles;

import jarcode.consoles.bungee.ConsoleBungeeHook;
import jarcode.consoles.command.*;
import jarcode.consoles.util.sync.SyncTaskScheduler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

public class Consoles extends JavaPlugin {

	public static final boolean DEBUG = true;

	private static Consoles instance;
	{
		instance = this;
	}
	public static Consoles getInstance() {
		return instance;
	}

	private CommandHandler commandHandler;

	@Override
	public void onEnable() {
		commandHandler = new CommandHandler(CommandConsole.class, CommandImage.class);
		boolean forward = getConfig().getBoolean("bungee-forward", false);
		ConsoleHandler.getInstance().local = !forward;
		if (DEBUG)
			try {
				commandHandler.addCommand(CommandMapTest.class);
			} catch (IllegalAccessException | InstantiationException e) {
				e.printStackTrace();
			}
		register(
				ConsoleHandler::getInstance, ConsoleBungeeHook::new, SyncTaskScheduler::create,
				this::getCommandHandler, ImageConsoleHandler::new
		);
		ImageConsoleHandler imageHandler = new ImageConsoleHandler();
		getServer().getScheduler().scheduleSyncDelayedTask(this, imageHandler::load);
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
