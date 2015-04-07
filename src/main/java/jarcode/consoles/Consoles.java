package jarcode.consoles;

import jarcode.consoles.bungee.ConsoleBungeeHook;
import jarcode.consoles.util.sync.SyncTaskScheduler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

public class Consoles extends JavaPlugin {
	private static Consoles instance;
	{
		instance = this;
	}
	public static Consoles getInstance() {
		return instance;
	}
	@Override
	public void onEnable() {
		register(
				ConsoleHandler::getInstance, ConsoleBungeeHook::new, SyncTaskScheduler::create
		);
		new ImageConsoleHandler();
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
			
		});
	}
	private void register(Supplier... suppliers) {
		for (Supplier supplier : suppliers) {
			Object obj = supplier.get();
			if (obj instanceof Listener)
				this.getServer().getPluginManager().registerEvents((Listener) obj, this);
		}
	}
}
