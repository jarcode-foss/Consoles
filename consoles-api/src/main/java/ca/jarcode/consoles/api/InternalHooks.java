package ca.jarcode.consoles.api;

import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
	 * Handle for implementing code
	 */
	public static Supplier<Short> INTERNAL_ALLOC = null;
	/**
	 * Handle for implementing code
	 */
	public static Consumer<Short> INTERNAL_FREE = null;
	/**
	 * Handle for implementing code
	 */
	public static TranslateFunction INTERNAL_TRANSLATE = null;
	/**
	 * Handle for implementing code
	 */
	public static TranslateStringFunction INTERNAL_TRANSLATE_STRING = null;
	/**
	 * Handle for implementing code
	 */
	public static SendMapPacketFunction INTERNAL_SEND_PACKET = null;
	/**
	 * Handle for implementing code
	 */
	public static Function<ItemFrame, Boolean> INTERNAL_IS_CONSOLE_ENTITY = null;
	/**
	 * Handle for implementing code
	 */
	public static Function<Integer, Boolean> INTERNAL_IS_CONSOLE_ENTITY_ID = null;

	/**
	 * Interface for implementing code
	 */
	@FunctionalInterface
	public interface TranslateFunction {
		int translate(Player player, short global);
	}

	/**
	 * Interface for implementing code
	 */
	@FunctionalInterface
	public interface TranslateStringFunction {
		int translate(String player, short global);
	}

	/**
	 * Interface for implementing code
	 */
	@FunctionalInterface
	public interface SendMapPacketFunction {
		void sendMapPacket(byte[] data, Player player, int id);
	}

	/**
	 * Allocates a new global map ID.
	 *
	 * @return a new global map ID.
	 */
	public static short alloc() {
		return INTERNAL_ALLOC.get();
	}

	/**
	 * Frees a global map ID.
	 *
	 * @param global the map ID to free.
	 */
	public static void free(short global) {
		INTERNAL_FREE.accept(global);
	}

	/**
	 * Translates a global map ID to the client ID for a given player
	 *
	 * @param player the player to translate for
	 * @param global the global ID to lookup
	 * @return the client map ID for the given player
	 */
	public static int translate(Player player, short global) {
		return INTERNAL_TRANSLATE.translate(player, global);
	}

	/**
	 * Translates a global map ID to the client ID for a given player
	 *
	 * @param player the player to translate for
	 * @param global the global ID to lookup
	 * @return the client map ID for the given player
	 */
	public static int translate(String player, short global) {
		return INTERNAL_TRANSLATE_STRING.translate(player, global);
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
		INTERNAL_SEND_PACKET.sendMapPacket(data, player, id);
	}

	/**
	 * Returns whether the supplied item frame is part of a console or not
	 *
	 * @param entity the entity to check
	 * @return whether the {@link org.bukkit.entity.ItemFrame} is part of a console
	 */
	public static boolean isConsoleEntity(ItemFrame entity) {
		return INTERNAL_IS_CONSOLE_ENTITY.apply(entity);
	}

	/**
	 * Returns whether the supplied entity id is part of a console or not
	 *
	 * @param id the entity ID to check
	 * @return whether the entity id corresponds to a console entity (item frame)
	 */
	public static boolean isConsoleEntity(int id) {
		return INTERNAL_IS_CONSOLE_ENTITY_ID.apply(id);
	}
}
