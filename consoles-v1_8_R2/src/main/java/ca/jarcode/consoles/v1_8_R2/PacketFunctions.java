package ca.jarcode.consoles.v1_8_R2;

import ca.jarcode.consoles.api.InternalHooks;
import com.google.common.collect.BiMap;
import net.minecraft.server.v1_8_R2.*;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * An extremely hack-y class for manipulating packets in craftbukkit without the use
 * of a protocol library.
 *
 * @author Jarcode
 */
public class PacketFunctions {


	// we ignore a decent amount of fields for map packets there because we can leave them as their defaults (0).
	private static final Field MAP_ID;
	private static final Field MAP_ICONS;
	private static final Field MAP_WIDTH;
	private static final Field MAP_HEIGHT;
	private static final Field MAP_DATA;

	static {
		try {
			// set up fields that we access when creating new packets
			MAP_ID = PacketPlayOutMap.class.getDeclaredField("a");
			MAP_ICONS = PacketPlayOutMap.class.getDeclaredField("c");
			MAP_WIDTH = PacketPlayOutMap.class.getDeclaredField("f");
			MAP_HEIGHT = PacketPlayOutMap.class.getDeclaredField("g");
			MAP_DATA = PacketPlayOutMap.class.getDeclaredField("h");
			MAP_ID.setAccessible(true);
			MAP_ICONS.setAccessible(true);
			MAP_WIDTH.setAccessible(true);
			MAP_HEIGHT.setAccessible(true);
			MAP_DATA.setAccessible(true);
		}
		catch (Throwable e) {
			throw new RuntimeException("Could not initialize packet fields");
		}
	}

	// faster way of creating maps
	// we avoid copying a buffer when creating the packet using reflection,
	// so we can use the pixel buffer's sections directly.
	public static PacketPlayOutMap createUpdatePacket(byte[] data, int id, List<Packet> packets) {
		if (data == null) return null;
		PacketPlayOutMap map = newMapPacket(packets);
		try {
			// map damage value
			MAP_ID.set(map, id);
			// initialize the icon array in the packet with an empty array
			// we don't use any map icons, so this is fine to do.
			MAP_ICONS.set(map, new MapIcon[0]);
			// we always send packets that update the entire map area,
			// so the dimensions are always 128x128
			MAP_WIDTH.set(map, 128);
			MAP_HEIGHT.set(map, 128);
			// pass through the byte array directly
			// this avoids a considerable amount of overhead from sending packets
			MAP_DATA.set(map, data);
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return map;
	}


	private static final Field PACKET_LIST;
	private static final Field PACKET_ENTITY_ID;

	static {
		try {
			PACKET_LIST = PacketPlayOutEntityMetadata.class.getDeclaredField("b");
			PACKET_LIST.setAccessible(true);
			PACKET_ENTITY_ID = PacketPlayOutEntityMetadata.class.getDeclaredField("a");
			PACKET_ENTITY_ID.setAccessible(true);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// we're going to block all map packets other than the ones we send. This is
	// a last stand defence against other plugins that may send map packets manually.

	// Most packets should be blocked by our fake map items and trackers, so this
	// won't do much in normal servers.
	public static boolean handleMapPacket(PacketPlayOutMap packet, List<Packet> packets) {
		if (packets.contains(packet)) {
			packets.remove(packet);
			return true;
		}
		else return false;
	}



	public static PacketPlayOutMap newMapPacket(List<Packet> packets) {
		PacketPlayOutMap packet = new PacketPlayOutMap();
		packets.add(packet);
		return packet;
	}


	// this baby translates map IDs in outgoing packets according to the player
	@SuppressWarnings("unchecked")
	public static boolean handleMetadataPacket(PacketPlayOutEntityMetadata packet, String context) {
		// get list of objects
		List<DataWatcher.WatchableObject> list = (List<DataWatcher.WatchableObject>) get(PACKET_LIST, packet);
		// create object mappings of the above list
		HashMap<Integer, DataWatcher.WatchableObject> objects = new HashMap<>();
		for (DataWatcher.WatchableObject aList : list) {
			objects.put(aList.a(), aList);
		}
		// get entity id
		int id;
		try {
			id = PACKET_ENTITY_ID.getInt(packet);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		// if the map/packet contains an item stack, and is for a console entity
		if (objects.containsKey(8) && InternalHooks.isConsoleEntity(id)) {
			// get stack from the map
			ItemStack stack = (ItemStack) objects.get(8).b();
			// global map id
			short global = (short) stack.getData();
			// player context map id
			short translated = (short) InternalHooks.translate(context, global);
			// set data of item stack
			if (translated != -1)
				stack.setData(translated);
			else return false;
		}
		// block other map metadata
		else if (objects.containsKey(8) && objects.get(8).b() instanceof ItemStack) {
			ItemStack stack = (ItemStack) objects.get(8).b();
			if (stack.getItem() == Items.FILLED_MAP || stack.getItem() == Items.MAP) {
				return false;
			}
		}
		return true;
	}

	private static Object get(Field field, Object instance) {
		try {
			return field.get(instance);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
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
	 * @deprecated broken code
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
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
