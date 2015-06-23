package jarcode.consoles.api;

import org.bukkit.entity.Player;

public interface CanvasComponent {
	/**
	 * Returns the width of this component
	 *
	 * @return the width of this component
	 */
	public int getWidth();

	/**
	 * Returns the height of this component
	 *
	 * @return the height of this component
	 */
	public int getHeight();

	/**
	 * Returns whether this component belonged or belongs to a container
	 *
	 * @return true if this component belonged to a container, false if not
	 */
	public boolean isContained();

	/**
	 * Returns the background color for this component
	 *
	 * @return the background color
	 */
	public byte getBackground();

	/**
	 * Sets the background color for this component
	 *
	 * @param bg the background color
	 */
	public void setBackground(byte bg);

	/**
	 * Returns whether this component is enabled or not
	 *
	 * @return whether this component is enabled or not
	 */
	public boolean enabled();

	/**
	 * Sets whether this component is enabled or disabled
	 *
	 * @param enabled true for enabled, false for disabled
	 */
	public void setEnabled(boolean enabled);

	/**
	 * Handles a click event for this component. If this component is a container,
	 * all mapped underlying components will have their handlers called as well.
	 *
	 * @param x x position
	 * @param y y position
	 * @param player the player who interacted
	 */
	public void handleClick(int x, int y, Player player);
}
