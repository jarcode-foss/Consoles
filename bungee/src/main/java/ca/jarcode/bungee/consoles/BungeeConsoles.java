package ca.jarcode.bungee.consoles;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Plugin for handling per-player sets of map IDs across a network of servers
 */
public class BungeeConsoles extends Plugin {

	private static BungeeConsoles instance = null;

	{
		instance = this;
	}

	public ConsoleMessageHandler handler;

	public static BungeeConsoles getInstance() {
		return instance;
	}

	public void onEnable() {
		handler = new ConsoleMessageHandler(this);
		getProxy().getPluginManager().registerListener(this, handler);
	}

	public ConsoleMessageHandler getMessageHandler() {
		return handler;
	}

	public static ProxiedPlayer getProxiedPlayer(Connection connection) {
		for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
			if (connection.getAddress().equals(player.getAddress())) {
				return player;
			}
		}
		return null;
	}
}
