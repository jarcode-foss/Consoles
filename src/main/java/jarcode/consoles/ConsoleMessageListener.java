package jarcode.consoles;

import org.bukkit.command.CommandSender;

/*

Interface applied to components to allow for listening to command input. Can
be from any command sender.

 */
public interface ConsoleMessageListener {
	public String execute(CommandSender sender, String text);
}
