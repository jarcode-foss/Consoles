package ca.jarcode.consoles.api;

import org.bukkit.entity.Player;

public interface CanvasContainer {
	/**
	 * Fires a click event for this container, without calling the
	 * handlers from the underlying components.
	 *
	 * @param x x position
	 * @param y y position
	 * @param player the player who interacted
	 */
	void onClick(int x, int y, Player player);

	/**
	 * Adds the given component to this container
	 *
	 * @param component the component to add
	 */
	void add(CanvasComponent component);

	/**
	 * Removes the given component from this container
	 *
	 * @param comp the component to remove
	 */
	void remove(CanvasComponent comp);

	/**
	 * Returns an array of all the components currently in
	 * this container.
	 *
	 * @return an array of components
	 */
	CanvasComponent[] components();
}
