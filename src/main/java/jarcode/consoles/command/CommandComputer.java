package jarcode.consoles.command;

import org.bukkit.command.CommandSender;

public class CommandComputer extends CommandBase {
	public CommandComputer() {
		super("pc");
	}

	@Override
	public boolean onCommand(CommandSender sender, String name, String message, String[] args) {
		return false;
	}
}
