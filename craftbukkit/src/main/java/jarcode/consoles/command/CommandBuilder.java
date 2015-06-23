package jarcode.consoles.command;

import jarcode.consoles.Consoles;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;

public class CommandBuilder {
	private final String name;
	private final ArrayList<String> aliases = new ArrayList<>();
	private Command command;
	private String node;
	private String desc;
	private boolean cancel = true;
	public static CommandBuilder create(String name) {
		return new CommandBuilder(name);
	}
	private CommandBuilder(String name) {
		this.name = name;
	}
	public CommandBuilder description(String desc) {
		this.desc = desc;
		return this;
	}
	public CommandBuilder command(Command command) {
		this.command = command;
		return this;
	}
	public CommandBuilder aliases(String... aliases) {
		this.aliases.addAll(Arrays.asList(aliases));
		return this;
	}
	public CommandBuilder node(String node) {
		this.node = node;
		return this;
	}
	public CommandBuilder cancelAfter(boolean cancel) {
		this.cancel = cancel;
		return this;
	}
	public BuiltCommand build() {
		return new BuiltCommand();
	}
	public class BuiltCommand extends CommandBase {

		private BuiltCommand() {
			super(name, aliases.toArray(new String[aliases.size()]));
			setNode(node);
			setDescription(desc);
			cancelAfter(cancel);
		}

		@Override
		public boolean onCommand(CommandSender sender, String name, String message, String[] args) {
			return command.onCommand(sender, name, message, args);
		}

		public Exception register(Consoles plugin) {
			try {
				plugin.registerCommand(this.getClass());
			} catch (IllegalAccessException | InstantiationException e) {
				return e;
			}
			return null;
		}
		public Exception register(CommandHandler handler) {
			try {
				handler.addCommand(this.getClass());
			} catch (IllegalAccessException | InstantiationException e) {
				return e;
			}
			return null;
		}
	}
}
