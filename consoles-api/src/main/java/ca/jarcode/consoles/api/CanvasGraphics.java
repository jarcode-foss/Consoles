package ca.jarcode.consoles.api;

import org.bukkit.map.MapFont;

/**
 * This class is used as a mutable graphics instance that is
 * used to paint pixels that belong to a particular component.
 */
public interface CanvasGraphics {

	/**
	 * Draws formatted text using vanilla formatting codes
	 *
	 * @param x x position
	 * @param y y position
	 * @param inherit the default color
	 * @param text the text to draw
	 * @return the last color in the formatted text
	 */
	byte drawFormatted(int x, int y, byte inherit, String text);

	/**
	 * Sets the font to use when measuring and painting strings.
	 *
	 * @param font the font to use
	 */
	void setFont(MapFont font);

	/**
	 * Draws formatted text using vanilla formatting codes
	 *
	 * @param x x position
	 * @param y y position
	 * @param text the text to draw
	 * @return the last color in the formatted text
	 */
	byte drawFormatted(int x, int y, String text);

	/**
	 * Sets the coordinates of this graphics object either relative to the
	 * origin of this component (true), or the coordinates of the containing
	 * component (false).
	 *
	 * @param relative whether to use relative coordinates or not
	 */
	void setRelative(boolean relative);

	/**
	 * Y coordinate of this component's container
	 *
	 * @return the y coordinate
	 */
	int containerY();

	/**
	 * X coordinate of this component's container
	 *
	 * @return the x coordinate
	 */
	int containerX();

	/**
	 * Trims the text the the given length, in pixels
	 *
	 * @param text the text to parse
	 * @param len the length to trim the text to
	 * @return a new, formatted string
	 */
	String trim(String text, int len);

	/**
	 * Width of the component this graphics object belongs to
	 *
	 * @return width, in pixels
	 */
	int getWidth();

	/**
	 * Height of the component this graphics object belongs to
	 *
	 * @return height, in pixels
	 */
	int getHeight();

	/**
	 * Returns the component this graphics object belong to
	 *
	 * @return the {@link ca.jarcode.consoles.api.CanvasComponent} object
	 */
	CanvasComponent getComponent();

	/**
	 * Returns the position of this component's container
	 *
	 * @return position of this component's container
	 */
	Position2D containerPosition();

	/**
	 * Returns a new instance of this object with the given relative coordinates.
	 *
	 * @param component the new component to draw for
	 * @param x x coordinate, relative to this object
	 * @param y y coordinate, relative to this object
	 * @return a new instance of {@link CanvasGraphics}
	 */
	CanvasGraphics subInstance(CanvasComponent component, int x, int y);

	/**
	 * Samples the color at the given location
	 *
	 * @param x x position
	 * @param y y position
	 * @return the byte representation of the color at the coordinate
	 */
	byte sample(int x, int y);

	/**
	 * Returns a new instance of this object with the given relative position.
	 *
	 * @param component the new component to draw for
	 * @param pos position, relative to this object
	 * @return a new instance of {@link CanvasGraphics}
	 */
	CanvasGraphics subInstance(CanvasComponent component, Position2D pos);

	/**
	 * Draws the given text with the given color code
	 *
	 * @param x x position
	 * @param y y position
	 * @param color color to draw
	 * @param text text to draw
	 */
	void draw(int x, int y, byte color, String text);

	/**
	 * Sets the pixel at the given point to the given color
	 *
	 * @param x x position
	 * @param y y position
	 * @param color color to set
	 */
	void draw(int x, int y, byte color);

	/**
	 * Draws the given section with the background buffer
	 *
	 * @param x x position
	 * @param y y position
	 * @param w width
	 * @param h height
	 */
	void drawBackground(int x, int y, int w, int h);

	/**
	 * Draws the background under this component
	 */
	void drawBackground();
}
