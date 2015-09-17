package ca.jarcode.consoles.messaging;

import com.google.common.io.ByteArrayDataOutput;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface OutgoingHookCommand {
	void handle(Player player, Object[] args, ByteArrayDataOutput out);
}
