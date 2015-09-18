package ca.jarcode.consoles.internal;

import ca.jarcode.consoles.api.nms.ClientConnection;
import ca.jarcode.consoles.api.nms.ConsolesNMS;

import java.util.HashMap;

/*

This class is for a single 128x128 map, it handles the actual sending of packets
and update triggers.

 */
public class ConsoleMapRenderer {

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
	public boolean update(ClientConnection connection, final String context) {

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
			Object packet = ConsolesNMS.packetInternals.createMapPacket(data, clientId);
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
	// this was before I even split NMS code, but I'm keeping this around just in case
	/*
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
	*/
}
