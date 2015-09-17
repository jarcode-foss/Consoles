package ca.jarcode.consoles.computer;
import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.internal.ConsoleHandler;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class GeneralListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	@SuppressWarnings("unused")
	public void onPreCommand(PlayerCommandPreprocessEvent e) {

		if (e.getMessage().startsWith("/")
				&& !e.getMessage().substring(1).trim().isEmpty()
				&& Computers.commandPrefix.equals("/")) {
			ComputerHandler handler = ComputerHandler.getInstance();
			if (handler != null && ConsoleHandler.getInstance().hittingConsole(e.getPlayer())) {
				handler.command(e.getMessage().substring(1), e.getPlayer());
				e.setCancelled(true);
			}
		}
	}


	@EventHandler(priority = EventPriority.HIGHEST)
	@SuppressWarnings("unused")
	public void onChat(AsyncPlayerChatEvent e) {
		if (e.getMessage().startsWith(Computers.commandPrefix) && !Computers.commandPrefix.equals("/")) {
			e.setCancelled(true);
			Bukkit.getScheduler().scheduleSyncDelayedTask(Computers.getInstance(), () -> {
				ComputerHandler handler = ComputerHandler.getInstance();
				if (handler != null && ConsoleHandler.getInstance().hittingConsole(e.getPlayer())) {
					handler.command(e.getMessage().substring(Computers.commandPrefix.length()), e.getPlayer());
				}
			});
		}
	}
}
