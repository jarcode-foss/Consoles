package jarcode.consoles.util;

import com.google.common.collect.BiMap;
import net.minecraft.server.v1_8_R2.*;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Predicate;

/**
 * An extremely hack-y class for manipulating packets in craftbukkit without the use
 * of a protocol library.
 *
 * @author Jarcode (Levi Webb)
 */
public class PacketUtils {
	public static boolean debugPackets(Player player) {
		final String name = player.getName();
		return registerInListener(Packet.class, player, packet -> {
			if (!(packet instanceof PacketPlayInKeepAlive)
					&& !(packet instanceof PacketPlayInFlying.PacketPlayInLook)
					&& !(packet instanceof PacketPlayInFlying.PacketPlayInPositionLook)
					&& !(packet instanceof PacketPlayInFlying.PacketPlayInPosition)
					&& !(packet instanceof PacketPlayInFlying))
				System.out.println("[PACKET] Packet from player " + name + ": " + packet.getClass());
			return true;
		});
	}
	/**
	 * Register a listener for an incoming packet type. The type may be abstract.
	 *
	 * The listeners registered will be fired in the inverse order that they were registered;
	 * last fired first, and the first fired last (with the default server handler last).
	 *
	 * Listeners that return false will not fire any other handlers afterwards.
	 *
	 * Also, remember packets are handled in a separate thread! Make sure to use schedulers if you
	 * need to call Bukkit-related code, or just synchronize your listener implementation (avoid blocking).
	 *
	 * This only works for incoming (server-bound) packets.
	 *
	 * @param type the type of the packet
	 * @param player the player to register the listener for
	 * @param listener the packet listener function
	 * @param <T> the type of the packet
	 * @return true if the listener was successfully registered, false if not.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Packet> boolean registerInListener(final Class<T> type,
	                                                            Player player,
	                                                            Predicate<T> listener) {
		PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
		PacketListener old = connection.networkManager.getPacketListener();
		if (!(old instanceof PacketListenerPlayIn && old instanceof PacketListenerPlayOut)) return false;
		connection.networkManager.a(new CustomPacketListener<>(type, listener, old));
		return true;
	}

	/**
	 * Replaces a packet in the Minecraft protocol. All packets send/relieved with the
	 * replaced packet type will use the new packet class instead.
	 *
	 * Make sure to implement safe and protocol-compliant packets.
	 *
	 * @param original The packet to replace
	 * @param custom The custom packet to use
	 * @param direction The protocol direction
	 * @param <T> Base packet type
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Packet> void replacePacket(Class<T> original, Class<? extends T> custom,
	                                                    ProtocolDirection direction) {
		try {
			// packet mappings for protocol state
			Field map = EnumProtocol.class.getDeclaredField("h");
			map.setAccessible(true);
			// get the map for this protocol
			BiMap protocolMap = ((BiMap) ((Map) map.get(EnumProtocol.PLAY)).get(direction.getNMS()));
			// get the id of the packet we're using
			Integer id = (Integer) protocolMap.inverse().get(original);
			// replace the packet with our own
			protocolMap.put(id, custom);
		}
		// if we can an exception (security or the field doesn't exist), then throw a runtime exception
		// we're reliant on this code to have packet modification, so we need this to work.
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * Register a listener for an incoming packet type. The type may be abstract.
	 *
	 * The listeners registered will be fired in the inverse order that they were registered;
	 * last fired first, and the first fired last (with the default server handler last).
	 *
	 * Listeners that return false will not fire any other handlers afterwards.
	 *
	 * Also, remember packets are handled in a separate thread! Make sure to use schedulers if you
	 * need to call Bukkit-related code, or just synchronize your listener implementation (avoid blocking).
	 *
	 * This method also requires that the player's network manager to be wrapper first. If it is not already
	 * wrapped, this will be done first.
	 *
	 * This only works for outgoing (client-bound) packets.
	 *
	 * @param type the type of the packet
	 * @param player the player to register the listener for
	 * @param listener the packet listener function
	 * @param <T> the type of the packet
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Packet> void registerOutListener(final Class<T> type,
	                                                            Player player,
	                                                            Predicate<T> listener) {
		NetworkManagerWrapper wrapper = wrapNetworkManager(player);
		wrapper.registerOutgoingListener(type, listener);
	}
	/**
	 * This wraps the player's network manager and replaces the current network manager
	 * with the wrapped version.
	 *
	 * If the player's connection is already wrapped, nothing is changed and the current
	 * manager is returned
	 *
	 * @param player The player whose network manager is to be wrapped
	 * @return the instance of the wrapper
	 */
	public static NetworkManagerWrapper wrapNetworkManager(Player player) {
		try {
			PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
			if (connection.networkManager instanceof NetworkManagerWrapper)
				return (NetworkManagerWrapper) connection.networkManager;

			Field field = PlayerConnection.class.getDeclaredField("networkManager");

			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

			NetworkManagerWrapper wrapper = NetworkManagerWrapper.wrap(connection.networkManager);
			field.set(connection, wrapper);
			return wrapper;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	// wrapper for NMS protocol direction enums
	public enum ProtocolDirection {
		OUT(EnumProtocolDirection.CLIENTBOUND), IN(EnumProtocolDirection.SERVERBOUND);
		private EnumProtocolDirection nms;
		ProtocolDirection(EnumProtocolDirection nms) {
			this.nms = nms;
		}
		public EnumProtocolDirection getNMS() {
			return nms;
		}
	}
}
