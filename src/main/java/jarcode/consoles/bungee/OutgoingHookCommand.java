package jarcode.consoles.bungee;

import com.google.common.io.ByteArrayDataOutput;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface OutgoingHookCommand {
	public void handle(Player player, Object[] args, ByteArrayDataOutput out);
}
