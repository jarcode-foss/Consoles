package jarcode.consoles.command;

import jarcode.consoles.computer.ComputerHandler;
import jarcode.consoles.computer.ManagedComputer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandComputer extends CommandBase {
	public CommandComputer() {
		super("computer", "comp");
		setNode("computer.manage");
	}

	@Override
	public boolean onCommand(CommandSender sender, String name, String message, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You have to be a player to do that!");
			return true;
		}
		Player player = (Player) sender;

		if (args.length == 0) {
			printHelp(sender);
		}
		else if (args[0].equalsIgnoreCase("remove") && args.length >= 2) {
			ComputerHandler.getInstance().find(args[1]).destroy();
		}
		else if (args[0].equalsIgnoreCase("create") && args.length >= 3) {
			BlockFace face;
			switch (args[1].toLowerCase()) {
				case "n":
				case "north":
					face = BlockFace.NORTH;
					break;
				case "s":
				case "south":
					face = BlockFace.SOUTH;
					break;
				case "e":
				case "east":
					face = BlockFace.EAST;
					break;
				case "w":
				case "west":
					face = BlockFace.WEST;
					break;
				default:
					sender.sendMessage(ChatColor.RED + "Invalid direction: " + args[2]);
					return true;
			}

			StringBuilder builder = new StringBuilder();

			for (int t = 2; t < args.length; t++) {
				builder.append(args[t]);
				if (t != args.length - 1) {
					builder.append(' ');
				}
			}
			String str = builder.toString();

			ManagedComputer computer = new ManagedComputer(str, player.getUniqueId());
			computer.create(face, player.getLocation());
		}
		return true;
	}
	private void printHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "Computer command usage:");
		sender.sendMessage(ChatColor.YELLOW + "/computer list" + ChatColor.WHITE + " - " +
				"shows all your computers");
		sender.sendMessage(ChatColor.BLUE + "/computer remove [hostname]" + ChatColor.WHITE + " - " +
				"removes the computer with the given hostname");
		sender.sendMessage(ChatColor.BLUE + "/computer create [N/E/S/W] [hostname]" + ChatColor.WHITE + " - " +
				"removes the computer with the given hostname");
	}
}
