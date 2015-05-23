package jarcode.consoles;

import jarcode.consoles.util.MapInjector;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;

import java.lang.reflect.Field;
import java.util.HashMap;

/*

This class is for a single 128x128 map, it handles the actual sending of packets
and update triggers.

 */
public class ConsoleMapRenderer {

	// we ignore a decent amount of fields there because we can leave them as their defaults (0).
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
	private static PacketPlayOutMap createUpdatePacket(byte[] data, int id) {
		if (data == null) return null;
		PacketPlayOutMap map = new PacketPlayOutMap();
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

	private ConsoleRenderer renderer;
	private short id;
	private ConsolePixelBuffer pixelBuffer;
	private int x, y;
	private HashMap<String, Short> newContexts = new HashMap<>();

	public ConsoleMapRenderer(ConsolePixelBuffer pixelBuffer, int x, int y, ConsoleRenderer renderer, short id) {
		this.id = id;
		this.pixelBuffer = pixelBuffer;
		this.renderer = renderer;
		this.x = x;
		this.y = y;
	}
	public void clearContextCache(String context) {
		newContexts.remove(context);
	}
	public void forceSwitch(String context) {
		ConsolePixelBuffer.UpdateSwitch update = pixelBuffer.getSwitch(context, x, y);
		if (update != null)
			update.fire();
	}
	public boolean update(PlayerConnection connection, final String context) {

		if (!renderer.created()) return false;

		// this switch determines whether the packet should be sent to the client.
		// it is only true/fired if this section of the pixel buffer has been written to since the last check,
		// and returns null if the context has never been written to for the entire console
		ConsolePixelBuffer.UpdateSwitch update = pixelBuffer.getSwitch(context, x, y);

		// get the id of the map we're actually sending to, instead of the global id we generally refer to
		short clientId = ConsoleHandler.getInstance().translateIndex(context, id);
		// send an update if an update was fired, or if the context map id hasn't been seen to this player before
		short lastId = getLastId(context);
		if ((update != null && update.wasFired()) || lastId != clientId) {
			if (lastId == -1)
				return true;
			byte[] data = pixelBuffer.getBuffer(context, x, y);
			// if the id of the map for this player changed, send them an update packet
			if (lastId != clientId) {
				newContexts.put(context, clientId);
			}
			// create the packet
			PacketPlayOutMap packet = createUpdatePacket(data, clientId);
			// send the packet
			if (packet != null)
				connection.sendPacket(packet);
			return true;
		}
		return false;
	}
	private short getLastId(String context) {
		if (newContexts.containsKey(context))
			return newContexts.get(context);
		else return -2;
	}
	// old code used to manually update the metadata of an item frame
	@Deprecated
	@SuppressWarnings("unused")
	public void updateMap(final PlayerConnection connection, final short mapId) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			Integer entityId = renderer.entityMap().get(id);
			DataWatcher watcher = new DataWatcher(null);
			watcher.a(0, (byte) 0);
			watcher.a(1, (short) 0);
			watcher.a(8, mapOf(mapId));
			PacketPlayOutEntityMetadata update =
					new PacketPlayOutEntityMetadata(entityId, watcher, true);
			connection.sendPacket(update);
		});
	}
	public static ItemStack mapOf(short id) {
		MapInjector.overrideMap(id);
		return CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(org.bukkit.Material.MAP, 1, id));
	}
}
