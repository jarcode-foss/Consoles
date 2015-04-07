package jarcode.consoles.api;
import jarcode.consoles.Position2D;

public interface Canvas {
	public void setIdentifier(String identifier);
	public void remove();
	public void repaint();
	public void putComponent(Position2D position, CanvasComponent object);
	public void removeComponent(Position2D position);
	public void removeComponent(CanvasComponent object);
	public default void putComponent(int x, int y, CanvasComponent object) {
		putComponent(new Position2D(x, y), object);
	}
	public void drawBackground(boolean draw);
	public default CanvasComponentBuilder newComponent(int width, int height) {
		return new CanvasComponentBuilder(this, width, height);
	}
}
