package ca.jarcode.consoles.computer;
import ca.jarcode.consoles.api.nms.CommandInternals;
import ca.jarcode.consoles.api.nms.ConsolesNMS;
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

import static ca.jarcode.consoles.Lang.lang;

// This is a vanilla command. I need to use this because command blocks send commands
// as the console, and I need to get the command block that sent it.
public class LinkCommand implements CommandInternals.BlockCommand {
	@Override
	public String getCommand() {
		return "link";
	}

	@Override
	public String getUsage() {
		return "/link <hostname>";
	}

	@Override
	public void execute(CommandBlock block, String[] args) {
		if (args.length > 0) {
			if (!ComputerHandler.getInstance().hostnameTaken(args[0]))
				ConsolesNMS.commandInternals.sendMessage(block, lang.getString("command-link-fail"));
			ComputerHandler.getInstance().request(args[0], block);
			ConsolesNMS.commandInternals.sendMessage(block, lang.getString("command-link-sent"));
		} else {
			ConsolesNMS.commandInternals.sendMessage(block,
					String.format(lang.getString("command-link-usage"), getUsage()));
		}
	}

	// we inject a vanilla command intended to be ran by command blocks
	public static void registerLinkCommand() {
		ConsolesNMS.commandInternals.registerBlockCommand(new LinkCommand());
	}
}
