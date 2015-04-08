package jarcode.consoles.computer;

import net.minecraft.server.v1_8_R2.*;

import java.lang.reflect.Field;

// This is a vanilla command. I need to use this because command blocks send commands
// as the console, and I need to get the command block that sent it.
public class LinkCommand extends CommandAbstract {
	@Override
	public String getCommand() {
		return "link";
	}

	@Override
	public String getUsage(ICommandListener iCommandListener) {
		return "/link <hostname>";
	}
	@Override
	public void execute(ICommandListener iCommandListener, String[] strings) throws CommandException {
		if (iCommandListener instanceof CommandBlockListenerAbstract) {
			CommandBlockListenerAbstract tile = (CommandBlockListenerAbstract) iCommandListener;
			TileEntityCommand command;
			try {
				// in 1.8.3, the command listener for the tile entity is an anonymous class!
				// to work around this, we get the immediate class, and check for this$0,
				// which is the instance of the containing class.
				Field field = tile.getClass().getDeclaredField("this$0");
				field.setAccessible(true);
				command = (TileEntityCommand) field.get(tile);

			} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
				e.printStackTrace();
				throw new CommandException("Failed to obtain command block entity");
			}
			//TODO: finish
		}
		else throw new CommandException("A command block has to execute this command!");
	}
	// only command blocks are allowed to use this!
	public boolean canUse(ICommandListener var1) {
		return var1 instanceof CommandBlockListenerAbstract;
	}

	// I have no idea as to what this is for.
	@SuppressWarnings("NullableProblems")
	@Override
	public int compareTo(ICommand o) {
		return 0;
	}
}
