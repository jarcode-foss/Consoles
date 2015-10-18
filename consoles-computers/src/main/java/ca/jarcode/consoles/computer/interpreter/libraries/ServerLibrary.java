package ca.jarcode.consoles.computer.interpreter.libraries;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class ServerLibrary {
	// executes a command as the console with Bukkit.
	public void console(String command) {
		if (command.startsWith("/"))
			command = command.substring(1);
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
	}
	public Player[] bukkitPlayers() {
		return Bukkit.getOnlinePlayers().stream().toArray(Player[]::new);
	}
	public void broadcast(String message) {
		Bukkit.broadcastMessage(message);
	}
	public void log(String level, String message) {
		Bukkit.getLogger().log(Level.parse(level.toUpperCase()), message);
	}
	public void info(String message) {
		Bukkit.getLogger().info(message);
	}
	public boolean disablePlugin(String name) {
		Plugin plugin = findPlugin(name);
		if (plugin == null || !plugin.isEnabled())
			return false;
		Bukkit.getPluginManager().disablePlugin(plugin);
		return true;
	}
	public boolean enablePlugin(String name) {
		Plugin plugin = findPlugin(name);
		if (plugin == null || plugin.isEnabled())
			return false;
		Bukkit.getPluginManager().enablePlugin(plugin);
		return true;
	}
	public boolean unloadWorld(String name) {
		return Bukkit.unloadWorld(name, true);
	}
	public boolean loadWorld(String name) {
		File loc = new File(Bukkit.getWorldContainer(), name);
		if (loc.exists() && loc.isDirectory()) {
			Bukkit.createWorld(new WorldCreator(name));
			return true;
		}
		else return false ;
	}
	private Plugin findPlugin(String name) {
		for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
			if (p.getName().equals(name)) {
				return p;
			}
		}
		return null;
	}
	private class BukkitSender implements CommandSender {

		private LuaCommandResult result;

		public BukkitSender(LuaCommandResult result) {
			this.result = result;
		}

		@Override
		public void sendMessage(String s) {
			result.text.append(s);
			result.text.append('\n');
		}

		@Override
		public void sendMessage(String[] strings) {
			for (String str : strings) {
				result.text.append(str);
				result.text.append('\n');
			}
		}

		@Override
		public Server getServer() {
			return Bukkit.getServer();
		}

		@Override
		public String getName() {
			return "lua$sender";
		}

		@Override
		public boolean isPermissionSet(String s) {
			return true;
		}

		@Override
		public boolean isPermissionSet(Permission permission) {
			return true;
		}

		@Override
		public boolean hasPermission(String s) {
			return true;
		}

		@Override
		public boolean hasPermission(Permission permission) {
			return true;
		}

		@Override
		public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b) {
			return null;
		}

		@Override
		public PermissionAttachment addAttachment(Plugin plugin) {
			return null;
		}

		@Override
		public PermissionAttachment addAttachment(Plugin plugin, String s, boolean b, int i) {
			return null;
		}

		@Override
		public PermissionAttachment addAttachment(Plugin plugin, int i) {
			return null;
		}

		@Override
		public void removeAttachment(PermissionAttachment permissionAttachment) {}

		@Override
		public void recalculatePermissions() {}

		@Override
		public Set<PermissionAttachmentInfo> getEffectivePermissions() {
			return new HashSet<>();
		}

		@Override
		public boolean isOp() {
			return true;
		}

		@Override
		public void setOp(boolean b) {}
	}
	private class LuaCommandResult {
		private StringBuilder text;
		private int state;
		private String result;
		public String getMessages() {
			return text.toString();
		}
		public String result() {
			return result;
		}
		public int state() {
			return state;
		}
	}
}
