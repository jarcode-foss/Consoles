package ca.jarcode.consoles.computer.interpreter.libraries;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.interpreter.FunctionBind;
import ca.jarcode.consoles.computer.interpreter.Lua;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
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
	// executes a vanilla command with a custom sender. The function bind is called if a command result is set.
	// Allows for target selectors like '@e'.
	public LuaCommandResult run(String command, FunctionBind bind) {
		if (command.startsWith("/"))
			command = command.substring(1);
		LuaCommandResult result = new LuaCommandResult();
		Computer computer = Lua.context();
		CommandBlockListenerAbstract.executeCommand(new VanillaSender(computer.getConsole().getLocation(),
						bind == null ? null : () -> bind.call(result), result),
				new BukkitSender(result), command);
		return result;
	}
	public LuaCommandResult vanilla(String command) {
		return run(command, null);
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

	private class VanillaSender implements ICommandListener {

		private Location location;
		private Runnable callback;
		private LuaCommandResult result;

		public VanillaSender(Location location, Runnable callback, LuaCommandResult result) {
			super();
			this.location = location;
			this.callback = callback;
			this.result = result;
		}

		@Override
		public String getName() {
			return "lua$listener";
		}

		@Override
		public IChatBaseComponent getScoreboardDisplayName() {
			return null;
		}

		@Override
		public void sendMessage(IChatBaseComponent iChatBaseComponent) {
			if (result != null) {
				result.text.append(iChatBaseComponent.getText());
				result.text.append('\n');
			}
		}

		// not sure what this does, copied from command block listener
		@Override
		public boolean a(int i, String s) {
			return i <= 2;
		}

		@Override
		public BlockPosition getChunkCoordinates() {
			return new BlockPosition(location.getBlockX() >> 4, location.getBlockY() >> 4, location.getBlockZ() >> 4);
		}

		@Override
		public Vec3D d() {
			return new Vec3D(location.getX(), location.getBlockY(), location.getZ());
		}

		@Override
		public World getWorld() {
			return ((CraftWorld) location.getWorld()).getHandle();
		}

		@Override
		public Entity f() {
			return null;
		}

		@Override
		public boolean getSendCommandFeedback() {
			return result != null;
		}

		@Override
		public void a(CommandObjectiveExecutor.EnumCommandResult enumCommandResult, int i) {
			if (result != null) {
				result.state = enumCommandResult.a();
				result.result = enumCommandResult.b();
				if (callback != null)
					callback.run();
			}
		}
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
