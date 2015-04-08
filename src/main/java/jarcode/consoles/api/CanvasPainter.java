package jarcode.consoles.api;

/**
 * A custom component painter
 */
@FunctionalInterface
public interface CanvasPainter {
	/**
	 * Paints this component with the given context and graphics instance.
	 * Painting is threaded, so it is unsafe to call functionality from the
	 * Bukkit API.
	 *
	 * @param g the graphics instance
	 * @param context the context (player) to paint for
	 */
	public void paint(CanvasGraphics g, String context);
}
