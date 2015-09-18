package ca.jarcode.consoles.api.nms;

import org.bukkit.entity.Player;

public interface PacketInternals {

	void registerMetadataPacketTranslator(Player player);
	void registerMapPacketFilter(Player player);
	void registerMapPacket(Object packet);
	ClientConnection getConnection(Player player);
	Object createMapPacket(byte[] data, int id);
}
