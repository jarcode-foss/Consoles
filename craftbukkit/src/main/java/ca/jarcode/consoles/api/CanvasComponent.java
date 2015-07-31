package ca.jarcode.consoles.api;

import org.bukkit.entity.Player;

public interface CanvasComponent {
	/**
	 * Returns the width of this component
	 *
	 * @return the width of this component
	 */
	int getWidth();

	/**
	 * Returns the height of this component
	 *
	 * @return the height of this component
	 */
	int getHeight();

	/**
	 * Returns whether this component belonged or belongs to a container
	 *
	 * @return true if this component belonged to a container, false if not
	 */
	boolean isContained();

	/**
	 * Returns the background color for this component
	 *
	 * @return the background color
	 */
	byte getBackground();

	/**
	 * Sets the background color for this component
	 *
	 * @param bg the background color
	 */
	void setBackground(byte bg);

	/**
	 * Returns whether this component is enabled or not
	 *
	 * @return whether this component is enabled or not
	 */
	boolean enabled();

	/**
	 * Sets whether this component is enabled or disabled
	 *
	 * @param enabled true for enabled, false for disabled
	 */
	void setEnabled(boolean enabled);

	/**
	 * Handles a click event for this component. If this component is a container,
	 * all mapped underlying components will have their handlers called as well.
	 *
	 * @param x x position
	 * @param y y position
	 * @param player the player who interacted
	 */
	void handleClick(int x, int y, Player player);
}
