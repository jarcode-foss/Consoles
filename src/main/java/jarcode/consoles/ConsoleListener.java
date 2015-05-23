package jarcode.consoles;

import org.bukkit.command.CommandSender;

/*

Interface applied to components to allow for listening to command input. Can
be from any command sender.

 */
public interface ConsoleListener {
	public String execute(CommandSender sender, String text);
}
