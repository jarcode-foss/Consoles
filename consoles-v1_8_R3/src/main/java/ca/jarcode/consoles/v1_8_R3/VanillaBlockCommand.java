package ca.jarcode.consoles.v1_8_R3;
import ca.jarcode.consoles.api.nms.CommandInternals;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.command.VanillaCommandWrapper;

import java.lang.reflect.Field;

// This is a vanilla command. I need to use this because command blocks send commands
// as the console, and I need to get the command block that sent it.
public class VanillaBlockCommand extends CommandAbstract {

	private CommandInternals.BlockCommand listener;

	public VanillaBlockCommand(CommandInternals.BlockCommand listener) {
		this.listener = listener;
	}

	@Override
	public String getCommand() {
		return listener.getCommand();
	}

	@Override
	public String getUsage(ICommandListener iCommandListener) {
		return listener.getUsage();
	}
	@Override
	public void execute(ICommandListener iCommandListener, String[] args) throws CommandException {
		if (iCommandListener instanceof CommandBlockListenerAbstract) {
			CommandBlockListenerAbstract tile = (CommandBlockListenerAbstract) iCommandListener;
			TileEntityCommand command;
			try {
				// in 1.8.3+, the command listener for the tile entity is an anonymous class!
				// to work around this, we get the immediate class, and check for this$0,
				// which is the instance of the containing class.
				Field field = tile.getClass().getDeclaredField("this$0");
				field.setAccessible(true);
				command = (TileEntityCommand) field.get(tile);

			} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
				e.printStackTrace();
				throw new CommandException("Failed to obtain command block entity ("
						+ e.getClass() + ":" + e.getCause() + ")");
			}
			BlockPosition pos = command.getPosition();
			CommandBlock block = (CommandBlock) command.getWorld().getWorld()
					.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getState();
			listener.execute(block, args);
		}
		else throw new CommandException("invalid source");
	}
	// only command blocks are allowed to use this!
	public boolean canUse(ICommandListener var1) {
		return var1 instanceof CommandBlockListenerAbstract;
	}

	// we inject a vanilla command intended to be ran by command blocks
	public static void registerLinkCommand(CommandInternals.BlockCommand listener) {
		SimpleCommandMap commandMap = ((CraftServer) Bukkit.getServer()).getCommandMap();
		commandMap.register("minecraft:", new VanillaCommandWrapper(new VanillaBlockCommand(listener)));
	}

	// I have no idea as to what this is for.
	@SuppressWarnings("NullableProblems")
	@Override
	public int compareTo(ICommand o) {
		return 0;
	}
	@Deprecated
	private void setOutput(String str, ChatColor color, TileEntityCommand entity) {

		JsonObject object = new JsonObject();
		object.add("text", new JsonPrimitive(str));
		object.add("color", new JsonPrimitive(color.name().toLowerCase()));

		IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(object.toString());
		try {
			Field field = CommandBlockListenerAbstract.class.getDeclaredField("d");
			field.setAccessible(true);
			field.set(entity.getCommandBlock(), component);
			entity.getCommandBlock().h();
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
