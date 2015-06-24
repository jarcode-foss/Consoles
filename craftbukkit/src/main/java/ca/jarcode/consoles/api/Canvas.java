package ca.jarcode.consoles.api;
import ca.jarcode.consoles.util.Position2D;

public interface Canvas {
	/**
	 * Sets the identifier for this canvas. Can be used to obtain an array
	 * of consoles with the same identifiers, and for debugging.
	 *
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(String identifier);

	/**
	 * Removes this canvas and the console it belongs to.
	 */
	public void remove();

	/**
	 * <p>Requests a repaint for this canvas. This does not guarantee that paint
	 * methods will actually be called.</p>
	 *
	 * <p>Paint methods are called when a player comes near the console, after any
	 * number of repaints has been scheduled.</p>
	 *
	 * <p>Updates, which are handled internally, are sent much more than repaints,
	 * like when a player joins, or when new map IDs are assigned to a player (on
	 * dimension changes), and are pulled from the buffer that stores the data
	 * from the last paint for the given context</p>
	 */
	public void repaint();

	/**
	 * Places a component at the given position in this canvas. This will remove
	 * another component at the given position, if it exists.
	 *
	 * @param position the position to place at
	 * @param object the component to place
	 */
	public void putComponent(Position2D position, CanvasComponent object);

	/**
	 * Removes the component at the specified position
	 *
	 * @param position the position to remove from
	 */
	public void removeComponent(Position2D position);

	/**
	 * Removes the first occurrence of this component in this canvas
	 *
	 * @param object the component to remove
	 */
	public void removeComponent(CanvasComponent object);

	/**
	 * Places a component at the given position in this canvas. This will remove
	 * another component at the given position, if it exists.
	 *
	 * @param x x position
	 * @param y y position
	 * @param object the component to place
	 */
	public default void putComponent(int x, int y, CanvasComponent object) {
		putComponent(new Position2D(x, y), object);
	}

	/**
	 * Toggles whether the background should be drawn or not for this canvas. The background
	 * data is re-cached when this is changed, so this method can be somewhat expensive.
	 *
	 * @param draw whether to draw the background for this canvas or not
	 */
	public void drawBackground(boolean draw);

	/**
	 * Returns a new component builder for this canvas
	 *
	 * @param width the width of the new component
	 * @param height the height of the new component
	 * @return a new {@link ca.jarcode.consoles.api.CanvasComponentBuilder}
	 */
	public default CanvasComponentBuilder newComponent(int width, int height) {
		return new CanvasComponentBuilder(this, width, height);
	}
}
