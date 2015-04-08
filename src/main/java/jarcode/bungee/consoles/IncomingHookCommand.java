package jarcode.bungee.consoles;

import com.google.common.io.ByteArrayDataInput;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public interface IncomingHookCommand {
	public void handle(ProxiedPlayer player, ByteArrayDataInput input);
}
