package jarcode.consoles.command;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface Command {
	public boolean onCommand(CommandSender sender, String name, String message, String[] args);
}
