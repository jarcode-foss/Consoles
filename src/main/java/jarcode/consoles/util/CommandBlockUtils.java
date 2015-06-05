package jarcode.consoles.util;

import jarcode.consoles.ConsoleMessageListener;
import net.minecraft.server.v1_8_R3.CommandBlockListenerAbstract;
import net.minecraft.server.v1_8_R3.TileEntityCommand;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftCommandBlock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/*

Util class for hacking at command blocks.

 */
public class CommandBlockUtils {

	public static final Field COMMAND_LISTENER;

	static {
		try {
			COMMAND_LISTENER = TileEntityCommand.class.getDeclaredField("a");
			COMMAND_LISTENER.setAccessible(true);
			overrideFinal(CommandBlockUtils.COMMAND_LISTENER);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isRegistered(CommandBlock block) {
		TileEntityCommand entity = ((CraftCommandBlock) block).getTileEntity();
		CommandBlockListenerAbstract obj = entity.getCommandBlock();
		return obj instanceof CommandBlockListenerWrapper && ((CommandBlockListenerWrapper) obj).listening();
	}

	public static boolean registerListener(CommandBlock block, ConsoleMessageListener listener) {
		TileEntityCommand entity = ((CraftCommandBlock) block).getTileEntity();
		CommandBlockListenerAbstract obj = entity.getCommandBlock();
		if (obj instanceof CommandBlockListenerWrapper && !isRegistered(block)) {
			((CommandBlockListenerWrapper) obj).setConsoleListener(listener);
			return true;
		}
		else return false;
	}

	public static boolean wrap(CommandBlock block) {
		try {
			TileEntityCommand entity = ((CraftCommandBlock) block).getTileEntity();
			CommandBlockListenerAbstract obj = entity.getCommandBlock();
			if (!(obj instanceof CommandBlockListenerWrapper)) {
				COMMAND_LISTENER.set(entity, new CommandBlockListenerWrapper(obj, entity));
				return true;
			}
			else return false;
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean restoreCommandBlock(CommandBlock block) {
		TileEntityCommand entity = ((CraftCommandBlock) block).getTileEntity();
		Object obj = entity.getCommandBlock();
		if (obj instanceof CommandBlockListenerWrapper) {
			((CommandBlockListenerWrapper) obj).setConsoleListener(null);
			return true;
		}
		else return false;
	}

	public static void overrideFinal(Field field) throws NoSuchFieldException, IllegalAccessException {
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		// remove the final flag on the security int/bytes
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
	}
}
