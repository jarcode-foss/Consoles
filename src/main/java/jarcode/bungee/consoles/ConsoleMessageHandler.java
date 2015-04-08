package jarcode.bungee.consoles;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ConsoleMessageHandler implements Listener {
	private final HashMap<UUID, List<Short>> blacklist = new HashMap<>();
	private final HashMap<String, Object> commands = new HashMap<>();
	private final List<UUID> toClear = new ArrayList<>();

	private Plugin plugin;

	{
		commands.put("ids", new HookCommand() {
			@Override
			public void handle(ProxiedPlayer player, ByteArrayDataInput input) {
				int s = input.readInt();
				short[] arr = new short[s];
				for (int t = 0; t < s; t++) {
					arr[t] = input.readShort();
				}
				blacklist(player.getUniqueId(), arr);
			}
			@SuppressWarnings("unchecked")
			@Override
			public void handle(ProxiedPlayer player, Object[] args, ByteArrayDataOutput out) {
				List<Short> arr = (List<Short>) args[0];
				out.writeInt(arr.size());
				for (short i : arr) {
					out.write(i);
				}
			}
		});
		commands.put("clear", (OutgoingHookCommand) (player, args, out) -> {});
	}

	private void blacklist(UUID uuid, short[] arr) {
		if (!blacklist.containsKey(uuid)) {
			blacklist.put(uuid, new ArrayList<>());
		}
		List<Short> list = blacklist.get(uuid);
		for (short s : arr) {
			if (!list.contains(s))
				list.add(s);
		}
	}

	public ConsoleMessageHandler(Plugin plugin) {
		this.plugin = plugin;
		plugin.getProxy().registerChannel("Console");
	}

	@EventHandler
	@SuppressWarnings("unused")
	public void onPluginMessage(PluginMessageEvent event) {
		if (event.getTag().equals("Console")) {
			ByteArrayDataInput input = ByteStreams.newDataInput(event.getData());
			String command = input.readUTF();
			Object cmd = commands.get(command.toLowerCase());
			if (cmd != null && cmd instanceof IncomingHookCommand) {
				((IncomingHookCommand) cmd).handle(BungeeConsoles.getProxiedPlayer(event.getReceiver()), input);
			}
		}
	}
	private void forwardFor(ProxiedPlayer player, Server server) {
		if (blacklist.containsKey(player.getUniqueId()))
			execute(player, server, "ids", blacklist.get(player.getUniqueId()));
	}
	@EventHandler
	@SuppressWarnings("unused")
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		if (blacklist.containsKey(event.getPlayer().getUniqueId()))
			blacklist.remove(event.getPlayer().getUniqueId());
		toClear.add(event.getPlayer().getUniqueId());
	}
	@EventHandler
	@SuppressWarnings("unused")
	public void onPlayerConnect(final ServerConnectedEvent event) {
		if (toClear.contains(event.getPlayer().getUniqueId())) {
			toClear.remove(event.getPlayer().getUniqueId());
			execute(event.getPlayer(), event.getServer(), "clear");
		}
		else forwardFor(event.getPlayer(), event.getServer());
	}
	public boolean execute(ProxiedPlayer player, String command, Object... args) {
		return execute(player, player.getServer(), command, args);
	}
	public boolean execute(ProxiedPlayer player, Server server, String command, Object... args) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		Object cmd = commands.get(command.toLowerCase());
		if (cmd != null && cmd instanceof OutgoingHookCommand) {
			out.writeUTF(command.toLowerCase());
			((OutgoingHookCommand) cmd).handle(player, args, out);
			server.sendData("Console", out.toByteArray());
			return true;
		}
		else return false;
	}
}
