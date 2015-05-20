package jarcode.consoles.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import jarcode.consoles.ManagedConsole;
import jarcode.consoles.ConsoleHandler;
import jarcode.consoles.Consoles;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashMap;

public class ConsoleBungeeHook implements PluginMessageListener, Listener {

	private final HashMap<String, Object> commands = new HashMap<>();
	private ConsoleHandler handler;

	private String resetServer = null;

	private boolean sentRequest = false;

	{
		commands.put("clear", (IncomingHookCommand) (player, input) -> {
			handler.clearAllocations(player);
			for (ManagedConsole console : handler.getConsoles())
				handler.getPainter().updateFor(console, player);
		});
	}

	public boolean needsRequestServerAddress() {
		boolean ret = (resetServer == null && !sentRequest);
		sentRequest = true;
		return ret;
	}

	public ConsoleBungeeHook() {
		this.handler = ConsoleHandler.getInstance();

		Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(Consoles.getInstance(), "Console");
		Bukkit.getServer().getMessenger().registerIncomingPluginChannel(Consoles.getInstance(), "Console", this);

		handler.setHook(this);
	}

	@Override
	public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
		if (s.equals("Console")) {
			ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
			String command = input.readUTF();
			Object cmd = commands.get(command.toLowerCase());
			if (cmd != null && cmd instanceof IncomingHookCommand) {
				((IncomingHookCommand) cmd).handle(player, input);
			}
		}
	}

	public void update(Player player) {
		execute(player, "getUpdate");
	}

	public boolean execute(Player player, String command, Object... args) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		Object cmd = commands.get(command);
		if (cmd != null && cmd instanceof OutgoingHookCommand) {
			out.writeUTF(command);
			((OutgoingHookCommand) cmd).handle(player, args, out);
			player.sendPluginMessage(Consoles.getInstance(), "Console", out.toByteArray());
			return true;
		}
		else return false;
	}
}
