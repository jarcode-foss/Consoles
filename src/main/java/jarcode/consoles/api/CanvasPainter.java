package jarcode.consoles.api;

@FunctionalInterface
public interface CanvasPainter {
	public void paint(CanvasGraphics g, String context);
}
