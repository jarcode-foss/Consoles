package jarcode.consoles.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import jarcode.consoles.ManagedConsole;
import jarcode.consoles.ConsoleHandler;
import jarcode.consoles.Consoles;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.Charset;
import java.util.HashMap;

public class ConsoleBungeeHook implements PluginMessageListener, Listener {

	private final HashMap<String, Object> commands = new HashMap<>();
	private ConsoleHandler handler;

	{
		commands.put("ids", new HookCommand() {
			@Override
			public void handle(Player player, ByteArrayDataInput input) {
				int s = input.readInt();
				short[] arr = new short[s];
				for (int t = 0; t < s; t++) {
					arr[t] = input.readShort();
				}
				handler.blacklist(player, arr);
				for (ManagedConsole console : handler.getConsoles())
					handler.getPainter().updateFor(console, player);
			}
			@Override
			public void handle(Player player, Object[] args, ByteArrayDataOutput out) {
				short[] arr = (short[]) args[0];
				out.writeInt(arr.length);
				for (short i : arr) {
					out.write(i);
				}
			}
		});
		commands.put("clear", (IncomingHookCommand) (player, input) -> {
			handler.clearAllocations(player);
			for (ManagedConsole console : handler.getConsoles())
				handler.getPainter().updateFor(console, player);
		});
	}

	public ConsoleBungeeHook() {
		this.handler = ConsoleHandler.getInstance();

		Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(Consoles.getInstance(), "Console");
		Bukkit.getServer().getMessenger().registerIncomingPluginChannel(Consoles.getInstance(), "Console", this);

		handler.setHook(this);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		if (!handler.local) {
			forwardIds(e.getPlayer(), handler.getContextIds(e.getPlayer()));
		}
	}
	@Override
	public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
		if (s.equals("Console")) {
			ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
			String command = input.readUTF();
			Object cmd = commands.get(command.toLowerCase());
			if (cmd != null && cmd instanceof IncomingHookCommand) {
				((IncomingHookCommand) cmd).handle(player, input);
				Bukkit.getLogger().info("Received message: " + cmd + ", for: " + player.getName());
				Bukkit.getLogger().info("data: " + new String(bytes, Charset.forName("UTF-8")));
			}
		}
	}
	public void forwardIds(String context, short... ids) {
		Player player = Bukkit.getPlayer(context);
		forwardIds(player, ids);
	}
	public void forwardIds(Player player, short... ids) {
		execute(player, "ids", new Object[] {ids});
	}
	public boolean execute(Player player, String command, Object... args) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		Object cmd = commands.get(command.toLowerCase());
		if (cmd != null && cmd instanceof OutgoingHookCommand) {
			out.writeUTF(command.toLowerCase());
			((OutgoingHookCommand) cmd).handle(player, args, out);
			player.sendPluginMessage(Consoles.getInstance(), "Console", out.toByteArray());
			return true;
		}
		else return false;
	}
}
