package ca.jarcode.consoles.api;

import ca.jarcode.consoles.internal.ConsoleHandler;
import ca.jarcode.consoles.internal.ConsoleMapRenderer;
import net.minecraft.server.v1_8_R3.PacketPlayOutMap;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * This class provides various internal hooks you can use to completely avoid
 * using the Consoles API.
 *
 * If you are using this class, it is important you know how consoles works.
 * Consoles works with dynamic map IDs and its own packet transformer to change
 * how clients perceive maps (a map with the same image may swap to a new id at
 * certain events). There is no way around this functionality.
 *
 * The server keeps track of both 'global' and 'client' map IDs. The global map
 * IDs stay the same, and correspond with the ID of the actual map on the server.
 * The client map ID is the ID that the client sees - PacketPlayOutMap and
 * PacketPlayOutEntityMetadata are modified so that players receive map updates
 * and maps in item frames according to the ID they should be viewing it from.
 *
 * The reason for this extensive system is to work around a client-sided issue
 * where maps will decide to stop updating completely when the player changes
 * dimension.
 *
 * You can hook right into this ID system if you want, however, you are left
 * to deal with how you translate map IDs to the client. Handheld maps would be
 * a near impossibility since you would need to change the damage value (ID) of
 * the map, differently for each client, when the client changes servers or
 * dimensions.
 *
 * Use this class with caution.
 *
 * @author Jarcode
 */
public class InternalHooks {

	/**
	 * Allocates a new global map ID.
	 *
	 * @return a new global map ID.
	 */
	public static short alloc() {
		return ConsoleHandler.getInstance().allocate(1);
	}

	/**
	 * Frees a global map ID.
	 *
	 * @param global the map ID to free.
	 */
	public static void free(short global) {
		ConsoleHandler.getInstance().free(global, 1);
	}

	/**
	 * Translates a global map ID to the client ID for a given player
	 *
	 * @param player the player to translate for
	 * @param global the global ID to lookup
	 * @return the client map ID for the given player
	 */
	public static int translate(Player player, short global) {
		return ConsoleHandler.getInstance().translateIndex(player.getName(), global);
	}

	/**
	 * Sends a map packet to the given player, with the given data. Consoles will translate
	 * the map ID to the client map ID when sending the packet.
	 *
	 * @param data a 128x128 single dimensional array, organized by a series of rows. You can
	 *             select data from the array using data[x + (y << 7)].
	 * @param player the player to send the packet to
	 * @param id the global ID of the map
	 */
	public static void sendMapPacket(byte[] data, Player player, int id) {
		PacketPlayOutMap packet = ConsoleMapRenderer.createUpdatePacket(data, id);
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
	}
}
