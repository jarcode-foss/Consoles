package jarcode.consoles;

import org.bukkit.command.CommandSender;

public interface ConsoleListener {
	public String execute(CommandSender sender, String text);
}
