package ca.jarcode.consoles.command;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface Command {
	boolean onCommand(CommandSender sender, String name, String message, String[] args);
}
