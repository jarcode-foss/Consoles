package ca.jarcode.consoles.v1_8_R2;

import ca.jarcode.consoles.api.nms.CommandExecutor;
import ca.jarcode.consoles.api.nms.CommandInternals;
import net.minecraft.server.v1_8_R2.ChatComponentText;
import net.minecraft.server.v1_8_R2.CommandBlockListenerAbstract;
import net.minecraft.server.v1_8_R2.TileEntityCommand;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_8_R2.block.CraftCommandBlock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.BooleanSupplier;

/*

Util class for hacking at command blocks.

 */
public class CommandBlockUtils implements CommandInternals {

	public final Field COMMAND_LISTENER;

	{
		try {
			COMMAND_LISTENER = TileEntityCommand.class.getDeclaredField("a");
			COMMAND_LISTENER.setAccessible(true);
			overrideFinal(COMMAND_LISTENER);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isRegistered(CommandBlock block) {
		TileEntityCommand entity = ((CraftCommandBlock) block).getTileEntity();
		CommandBlockListenerAbstract obj = entity.getCommandBlock();
		return obj instanceof CommandBlockListenerWrapper && ((CommandBlockListenerWrapper) obj).listening();
	}

	@Override
	public boolean registerListener(CommandBlock block, CommandExecutor listener) {
		TileEntityCommand entity = ((CraftCommandBlock) block).getTileEntity();
		CommandBlockListenerAbstract obj = entity.getCommandBlock();
		if (obj instanceof CommandBlockListenerWrapper && !isRegistered(block)) {
			((CommandBlockListenerWrapper) obj).setConsoleListener(listener);
			return true;
		}
		else return false;
	}

	@Override
	public void sendMessage(CommandBlock block, String message) {
		TileEntityCommand command = ((CraftCommandBlock) block).getTileEntity();
		command.getCommandBlock().sendMessage(new ChatComponentText(message));
	}

	@Override
	public void registerBlockCommand(BlockCommand listener) {
		VanillaBlockCommand.registerLinkCommand(listener);
	}

	@Override
	public boolean wrap(CommandBlock block, BooleanSupplier commandBlocksEnabled) {
		try {
			TileEntityCommand entity = ((CraftCommandBlock) block).getTileEntity();
			CommandBlockListenerAbstract obj = entity.getCommandBlock();
			if (!(obj instanceof CommandBlockListenerWrapper)) {
				COMMAND_LISTENER.set(entity, new CommandBlockListenerWrapper(obj, commandBlocksEnabled, entity));
				return true;
			}
			else return false;
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean restore(CommandBlock block) {
		TileEntityCommand entity = ((CraftCommandBlock) block).getTileEntity();
		Object obj = entity.getCommandBlock();
		if (obj instanceof CommandBlockListenerWrapper) {
			((CommandBlockListenerWrapper) obj).setConsoleListener(null);
			return true;
		}
		else return false;
	}

	public void overrideFinal(Field field) throws NoSuchFieldException, IllegalAccessException {
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		// remove the final flag on the security int/bytes
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
	}
}
