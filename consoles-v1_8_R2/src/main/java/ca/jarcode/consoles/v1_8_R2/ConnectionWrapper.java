package ca.jarcode.consoles.v1_8_R2;

import ca.jarcode.consoles.api.nms.ClientConnection;
import net.minecraft.server.v1_8_R2.Packet;
import net.minecraft.server.v1_8_R2.PlayerConnection;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class ConnectionWrapper implements ClientConnection {

	private PlayerConnection handle;

	public ConnectionWrapper(Player player) {
		handle = ((CraftPlayer) player).getHandle().playerConnection;
	}

	@Override
	public void sendPacket(Object packet) {
		handle.sendPacket((Packet) packet);
	}
}
