package ca.jarcode.consoles.api;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

/**
 * Represents a console in the game world
 */
public class Console {

	public static UnderlyingBuilder INTERNAL_CONSOLE_BUILDER = null;

	public interface UnderlyingBuilder {
		Canvas build(int w, int h, BlockFace face, Location location) throws ConsoleCreateException;
	}

	private final BlockFace face;
	private final Location location;
	private final int width, height;

	private boolean created = false;
	protected Canvas underlying = null;

	/**
	 * Wraps an internal console
	 *
	 * @param console the renderer to wrap
	 * @return the wrapped console
	 */
	public static Console wrap(Canvas console) {
		Console ret = new Console(console.getDirection(), console.getLocation(),
				console.getFrameWidth(), console.getFrameHeight());
		ret.created = true;
		ret.underlying = console;
		return ret;
	}

	/**
	 * Prepares a console to be created
	 *
	 * @param face the direction this canvas will face
	 * @param location the origin of the canvas
	 * @param width the width, in frames
	 * @param height the height, in frames
	 */
	public Console(BlockFace face, Location location, int width, int height) {
		this.location = location.clone();
		this.face = face;
		this.width = width;
		this.height = height;
	}

	/**
	 * Removes the console from the world
	 */
	public final void remove() {
		if (underlying != null)
			underlying.remove();
	}

	/**
	 * Gets the location of this console
	 *
	 * @return the location of the console
	 */
	public Location getLocation() {
		return location.clone();
	}

	/**
	 * Gets the width of this console, in frames
	 *
	 * @return the width, in frames
	 */
	public int getFrameWdith() {
		return width;
	}

	/**
	 * Gets the height of this console, in frames
	 *
	 * @return the height, in frames
	 */
	public int getFrameHeight() {
		return height;
	}

	/**
	 * Returns the {@link ca.jarcode.consoles.api.Canvas} for this object
	 *
	 * @return the {@link ca.jarcode.consoles.api.Canvas} for this object
	 */
	public final Canvas getCanvas() {
		return underlying;
	}

	/**
	 * Creates the console and adds it to the world.
	 *
	 * @return true if the console was created successfully, otherwise false
	 */
	public final boolean create() {
		if (created) return false;
		if (underlying != null) {
			underlying.remove();
		}
		try {
			underlying = INTERNAL_CONSOLE_BUILDER.build(width, height, face, location);
			created = true;
			return true;
		}
		catch (ConsoleCreateException e) {
			return false;
		}
	}

	/**
	 * Returns a component builder with the given dimensions
	 *
	 * @param width width of the resultant component
	 * @param height width of the resultant component
	 * @return a {@link CanvasComponentBuilder} with the given dimensions
	 */
	public CanvasComponentBuilder newComponent(int width, int height) {
		return getCanvas().newComponent(width, height);
	}
}
