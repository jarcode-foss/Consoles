package jarcode.consoles.command;

import com.google.common.collect.ObjectArrays;
import org.bukkit.command.CommandSender;

public abstract class CommandBase implements Command {
	private final String[] names;
	private String node = null;
	private String description = null;
	private boolean cancel = true;
	private CommandHandler handler = null;

	public void setCommandHandler(CommandHandler handler) {
		this.handler = handler;
	}

	public CommandHandler getHandler() {
		return handler;
	}

	public CommandBase(String name) {
		names = new String[] {name};
	}

	public CommandBase(String primary, String... aliases) {
		names = ObjectArrays.concat(primary, aliases);
	}

	public String[] getNames( ) {
		return names;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void cancelAfter(boolean cancel) {
		this.cancel = cancel;
	}

	public boolean cancelAfter() {
		return cancel;
	}

	public String getDescription() {
		return description;
	}

	public boolean hasNode(CommandSender sender) {
		return node == null || sender.hasPermission(node);
	}
}
