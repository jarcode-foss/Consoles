package ca.jarcode.consoles.internal;

import org.bukkit.command.CommandSender;

/*

Interface applied to components to allow for listening to command input. Can
be from any command sender.

 */
public interface ConsoleMessageListener {
	String execute(CommandSender sender, String text);
}
