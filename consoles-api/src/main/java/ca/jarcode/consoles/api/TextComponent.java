package ca.jarcode.consoles.api;

import org.bukkit.map.MapFont;

public interface TextComponent {

	/**
	 * Prints the given text to this component.
	 *
	 * @param text the text to print
	 */
	void print(String text);

	/**
	 * Sets the font for this component.
	 *
	 * @param font the new font to use
	 */
	void setFont(MapFont font);

	/**
	 * Clears all text from this component.
	 */
	void clear();

	/**
	 * Sets the color to use for the text.
	 *
	 * @param color the byte value of the color to use
	 */
	void setTextColor(byte color);
}
