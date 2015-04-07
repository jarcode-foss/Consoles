package jarcode.consoles.computer;

import net.minecraft.server.v1_8_R1.*;

import java.lang.reflect.Field;

// This is a vanilla command. I need to use this because command blocks send commands
// as the console. Bukkit is stupid sometimes.
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
	public void execute(ICommandListener iCommandListener, String[] strings) throws ExceptionUsage, CommandException {
		if (iCommandListener instanceof TileEntityCommandListener) {
			TileEntityCommandListener tile = (TileEntityCommandListener) iCommandListener;
			TileEntityCommand command;
			try {
				Field field = TileEntityCommandListener.class.getDeclaredField("a");
				field.setAccessible(true);
				command = (TileEntityCommand) field.get(tile);

			} catch (NoSuchFieldException | IllegalAccessException e) {
				e.printStackTrace();
				throw new CommandException("Failed to obtain command block entity");
			}
		}
		else throw new CommandException("A command block has to execute this command!");
	}

	// I have no idea as to what this is for.
	@SuppressWarnings("NullableProblems")
	@Override
	public int compareTo(Object ignored) {
		return 0;
	}
	// only command blocks are allowed to use this!
	public boolean canUse(ICommandListener var1) {
		return var1 instanceof TileEntityCommandListener;
	}
}
