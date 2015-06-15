package jarcode.consoles.command;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.ComputerHandler;
import jarcode.consoles.computer.ManagedComputer;
import jarcode.consoles.internal.ConsoleCreateException;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

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
		else if (args[0].equalsIgnoreCase("list")) {
			sender.sendMessage(ChatColor.BLUE + "Computers:");
			String[] messages = ComputerHandler.getInstance().getComputers().stream()
					.map(comp -> ChatColor.YELLOW + comp.getHostname()
							+ ":\n\towner: " + comp.getOwner() + "\n"
							+ ":\n\tlocation: " + formatLocation(comp.getConsole().getLocation()))
					.map(str -> str.replace("\t", "    "))
					.collect(Collectors.joining("\n"))
					.split("\n");
			sender.sendMessage(messages);
		}
		else if (args[0].equalsIgnoreCase("remove") && args.length >= 2) {
			Computer computer = ComputerHandler.getInstance().find(args[1]);
			if (computer == null) {
				sender.sendMessage(ChatColor.RED + "That computer doesn't exist!");
				return true;
			}
			computer.destroy(true);
		}
		else if (args[0].equalsIgnoreCase("give")) {
			player.getInventory().addItem(ComputerHandler.newComputerStack());
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
			try {
				computer.create(face, player.getLocation());
			} catch (ConsoleCreateException e) {
				computer.destroy(true);
				sender.sendMessage(ChatColor.YELLOW + "Failed to create computer at location " +
						"(cancelled by external plugin)");
				if (Consoles.debug)
					e.printStackTrace();
			}
		}
		return true;
	}
	private String formatLocation(Location location) {
		return String.format("(%d, %d, %d, %s)", location.getBlockX(),
				location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
	}
	private void printHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "Computer command usage:");
		sender.sendMessage(ChatColor.BLUE + "/computer list" + ChatColor.WHITE + " - " +
				"shows all your computers");
		sender.sendMessage(ChatColor.BLUE + "/computer give" + ChatColor.WHITE + " - " +
				"gives you a computer");
		sender.sendMessage(ChatColor.BLUE + "/computer remove [hostname]" + ChatColor.WHITE + " - " +
				"removes the computer with the given hostname");
		sender.sendMessage(ChatColor.BLUE + "/computer create [N/E/S/W] [hostname]" + ChatColor.WHITE + " - " +
				"removes the computer with the given hostname");
	}
}
