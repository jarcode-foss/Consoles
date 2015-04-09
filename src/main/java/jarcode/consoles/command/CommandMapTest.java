package jarcode.consoles.command;

import jarcode.consoles.*;
import jarcode.consoles.api.*;
import jarcode.consoles.util.PacketUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

// this is for testing
public class CommandMapTest extends CommandBase {

	public CommandMapTest() {
		super("maptest");
		setNode("consoles.maptest");
		setDescription("tests stuff");
	}
	@Override
	@SuppressWarnings("deprecation")
	public boolean onCommand(final CommandSender sender, String name, String message, String[] args) {
		Player player = (Player) sender;

		if (args.length == 0) {
			sender.sendMessage("/maptest [NORTH/SOUTH/EAST/WEST/CLEAR/LINK] s{extra}");
			return true;
		}
		if (args[0].equalsIgnoreCase("clear")) {
			ConsoleHandler.getInstance().get("maptest")
					.forEach(ManagedConsole::remove);
			sender.sendMessage("Removed all test consoles.");
			return true;
		}
		if (args[0].equalsIgnoreCase("repaintall")) {
			for (ManagedConsole console : ConsoleHandler.getInstance().getConsoles())
				ConsoleHandler.getInstance().getPainter().repaintFor(console, player);
			return true;
		}
		if (args[0].equalsIgnoreCase("forceall")) {
			for (ManagedConsole console : ConsoleHandler.getInstance().getConsoles())
				ConsoleHandler.getInstance().getPainter().updateFor(console, player, true, true);
			return true;
		}
		if (args[0].equalsIgnoreCase("updateall")) {
			for (ManagedConsole console : ConsoleHandler.getInstance().getConsoles())
				ConsoleHandler.getInstance().getPainter().updateFor(console, player, false, true);
			return true;
		}
		if (args[0].equalsIgnoreCase("sendall")) {
			for (ManagedConsole console : ConsoleHandler.getInstance().getConsoles())
				ConsoleHandler.getInstance().getPainter().updateFor(console, player, false, false);
			return true;
		}
		if (args[0].equalsIgnoreCase("debug")) {
			PacketUtils.debugPackets(player);
			return true;
		}
		if (args[0].equalsIgnoreCase("api")) {
			Console console = new Console(BlockFace.NORTH, ((Player) sender).getLocation(), 4, 5);
			CanvasComponent component = console.newComponent(4 * 128, 5 * 128).listen((x, y, player1) -> {

			}).enabledHandler(() -> true, aBoolean -> {}).painter((g, context) -> {
				for (int x = 0; x < g.getWidth(); x++) {
					for (int y = 0; y < g.getHeight(); y++) {
						g.draw(x, y, (byte) 60);
					}
				}
			}).create();
			console.getCanvas().putComponent(0, 0, component);
			console.create();
		}
		if (args[0].equalsIgnoreCase("link")) {
			BlockState block = player.getLocation().clone().add(0, -1, 0).getBlock().getState();
			if (block instanceof CommandBlock) {
				List<ManagedConsole> list = ConsoleHandler.getInstance().get("maptest");
				if (list.size() == 0) {
					sender.sendMessage("Spawn a console first!");
				}
				else {
					ManagedConsole console = list.get(0);
					ConsoleTextArea area = (ConsoleTextArea) console.getComponents()[0];
					area.link((CommandBlock) block);
				}
			}
			else
				sender.sendMessage("Must be standing on a command block.");
			return true;
		}
		args[0] = args[0].toUpperCase();
		BlockFace face;
		try {
			face = BlockFace.valueOf(args[0]);
		}
		catch (Throwable e) {
			sender.sendMessage("Invalid direction: " + args[0]);
			return true;
		}

		final ManagedConsole console = new ManagedConsole(4, 3);
		console.setIdentifier("maptest");
		console.create(face, player.getLocation());
		final ConsoleTextArea area = ConsoleTextArea.createOver(console);
		area.println("root@server-ubuntu:~$ apt-cache search minecraft");
		area.println("minetest - Multiplayer infinite-world block sandbox");
		area.println("minetest-data - Multiplayer infinite-world block sandbox (data files)");
		area.println("minetest-dbg - Multiplayer infinite-world block sandbox (debugging symbols)");
		area.println("minetest-server - Multiplayer infinite-world block sandbox (server)");
		area.println("root@server-ubuntu:~$ ping google.com");
		area.placeOver(console);
		player.sendMessage("Ta daa~!");
		for (int t = 0; t < 10; t++)
			Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
				Random random = new Random();
				area.println(String.format(ChatColor.RED + "64 bytes from google.com"
								+ ChatColor.WHITE + " (173.194.33.104): icmp_seq=3 ttl=58 time=%s ms",
						(random.nextInt(15) + 10) + "." + random.nextInt(10)));
				console.repaint();
			}, 20L * (t + 2));
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			area.println(ChatColor.GREEN + "Finished pinging google.com"
							+ ChatColor.WHITE + " 4 packets transmitted, 10 received, 0% packet loss, avg time 18ms"
					+ " - rtt min/avg/max/mdev = 18.210/18.451/18.671/0.273 ms");
			area.print("root@server-ubuntu:~$ _");
			console.repaint();
		}, 20L * 13);
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			ConsoleButton yes = new ConsoleButton(console, "Yes");
			ConsoleButton no = new ConsoleButton(console, "No");
			final ConsoleDialog dialog = ConsoleDialog.show(console, "Do you want to close this console?", yes, no);
			yes.addEventListener(event -> console.remove());
			no.addEventListener(event -> {
				dialog.setEnabled(false);
				console.repaint();
			});
			yes.addEventListener(event -> {
				dialog.setEnabled(false);
				console.repaint();
			});
			console.repaint();
		}, 20L * 16);
		return true;
	}

}
