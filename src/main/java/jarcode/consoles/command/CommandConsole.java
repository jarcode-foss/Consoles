package jarcode.consoles.command;

import jarcode.consoles.ManagedConsole;
import jarcode.consoles.ConsoleHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandConsole extends CommandBase {
	public CommandConsole() {
		super("console");
		setNode("consoles.console");
		setDescription("manages all console instances");
	}

	@Override
	public boolean onCommand(CommandSender sender, String name, String message, String[] args) {

		if (args.length == 0) {
			printHelp(sender);
		}
		else if (args[0].equalsIgnoreCase("list")) {
			sender.sendMessage(ChatColor.YELLOW + "Consoles:");
			int count = 1;
			for (ManagedConsole console : ConsoleHandler.getInstance().getConsoles()) {
				if (console == null)
					sender.sendMessage(count + " - " + ChatColor.GRAY + "null");
				else if (!console.created()) {
					sender.sendMessage(count + " - " + ChatColor.GRAY + "(not created)");
				}
				else
					sender.sendMessage(count + " - " + ChatColor.GRAY + console.getLocation().toString());
				count++;
			}
		}
		else if (args[0].equalsIgnoreCase("remove") && args.length >= 2) {
			int i;
			try {
				i = Integer.parseInt(args[1]);
			}
			catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.RED + "Invalid index: " + args[1]);
				return true;
			}
			i--;
			ManagedConsole[] arr = ConsoleHandler.getInstance().getConsoles();
			if (i >= arr.length || i < 0) {
				sender.sendMessage(ChatColor.RED + "Index too large or too small: " + args[1]);
				return true;
			}
			arr[i].remove();
			sender.sendMessage(ChatColor.YELLOW + "Removed console.");
		} else printHelp(sender);
		return true;
	}
	private void printHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "Console command usage:");
		sender.sendMessage(ChatColor.BLUE + "/console list" + ChatColor.WHITE + " - " +
				"lists the current consoles");
		sender.sendMessage(ChatColor.BLUE + "/console remove [index]" + ChatColor.WHITE + " - " +
				"removes a console at the given index");
		sender.sendMessage(ChatColor.RED + "This is a command for developers/debugging");
	}
}
