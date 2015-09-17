package ca.jarcode.consoles.messaging;

import com.google.common.io.ByteArrayDataInput;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface IncomingHookCommand {
	void handle(Player player, ByteArrayDataInput input);
}
