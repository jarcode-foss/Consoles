package ca.jarcode.consoles.api;

import org.bukkit.entity.Player;

/**
 * Interact listener for components
 */
@FunctionalInterface
public interface CanvasInteractListener {
	/**
	 * Fired when a player interacts with the canvas component. Coordinates are relative
	 * to the component origin.
	 *
	 * @param x the x coordinate that the player interacted with
	 * @param y the y coordinate that the player interacted with
	 * @param player the player who interacted with this component
	 */
	void handle(int x, int y, Player player);
}
