package ca.jarcode.consoles.v1_8_R3;

import ca.jarcode.consoles.api.nms.ClientConnection;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
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
