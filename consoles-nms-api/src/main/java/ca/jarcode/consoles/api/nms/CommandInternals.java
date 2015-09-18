package ca.jarcode.consoles.api.nms;

import org.bukkit.block.CommandBlock;

import java.util.function.BooleanSupplier;

public interface CommandInternals {

	boolean wrap(CommandBlock block, BooleanSupplier commandBlocksEnabled);
	boolean isRegistered(CommandBlock block);
	boolean restore(CommandBlock block);
	boolean registerListener(CommandBlock block, CommandExecutor listener);
	void sendMessage(CommandBlock block, String message);

	void registerBlockCommand(BlockCommand listener);

	interface BlockCommand {
		String getCommand();
		String getUsage();
		void execute(CommandBlock block, String[] args);
	}
}
