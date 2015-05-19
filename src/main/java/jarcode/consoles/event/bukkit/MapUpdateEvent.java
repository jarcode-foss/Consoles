package jarcode.consoles.event.bukkit;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MapUpdateEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final World world;

	private final int x, z, scale;

	public MapUpdateEvent(World world, int x, int z, int scale) {
		this.x = x;
		this.z = z;
		this.scale = scale;
		this.world = world;
	}

	public int getScale() {
		return scale;
	}

	public int getZ() {
		return z;
	}

	public int getX() {
		return x;
	}

	public World getWorld() {
		return world;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	@SuppressWarnings("unused")
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
