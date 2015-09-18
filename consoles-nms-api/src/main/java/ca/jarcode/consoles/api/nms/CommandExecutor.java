package ca.jarcode.consoles.api.nms;

import org.bukkit.command.CommandSender;

public interface CommandExecutor {
	String execute(CommandSender sender, String text);
}
