package jarcode.consoles.command;

import jarcode.consoles.ImageConsole;
import jarcode.consoles.ImageConsoleHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.MalformedURLException;
import java.net.URL;

public class CommandImage extends CommandBase {
	public CommandImage() {
		super("image", "im");
		setNode("consoles.image");
		setDescription("Renders data from URLs");
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
			URL url;
			try {
				url = new URL(str);
			}
			catch (MalformedURLException e) {
				sender.sendMessage(ChatColor.RED + "ERROR " + ChatColor.GRAY + '(' + e.getClass()
						+ ')' + ChatColor.WHITE + ": " + e.getMessage());
				sender.sendMessage(ChatColor.YELLOW + "Check the URL(s) you are entering (invalid URL?)");
				return true;
			}
			ImageConsole console = new ImageConsole(url, face, player.getLocation());
			console.create();
		}
		else if (args[0].equalsIgnoreCase("list")) {
			sender.sendMessage(ChatColor.YELLOW + "Images:");
			int count = 1;
			for (ImageConsole console : ImageConsoleHandler.getInstance().getImageConsoles()) {
				sender.sendMessage(count + " - " + console.getUrl().toString());
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
			int s = ImageConsoleHandler.getInstance().getImageConsoles().size();
			if (i >= s || i < 0) {
				sender.sendMessage(ChatColor.RED + "Index too large or too small: " + args[1]);
				return true;
			}
			ImageConsole console = ImageConsoleHandler.getInstance().getImageConsoles().get(i);
			console.remove();
			ImageConsoleHandler.getInstance().getImageConsoles().remove(i);
			ImageConsoleHandler.getInstance().save();
			sender.sendMessage(ChatColor.YELLOW + "Removed image.");
		} else printHelp(sender);
		return true;
	}
	private void printHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "Image command usage:");
		sender.sendMessage(ChatColor.BLUE + "/im create [N/E/S/W] [URL]" + ChatColor.WHITE + " - " +
				"creates an image at the current position for the given plugin");
		sender.sendMessage(ChatColor.BLUE + "/im list" + ChatColor.WHITE + " - " +
				"lists the current images");
		sender.sendMessage(ChatColor.BLUE + "/im remove [index]" + ChatColor.WHITE + " - " +
				"removes an image at the given index");
	}
}
