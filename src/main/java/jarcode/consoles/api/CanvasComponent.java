package jarcode.consoles.api;

import jarcode.consoles.ConsoleComponent;
import jarcode.consoles.ConsoleRenderer;
import org.bukkit.entity.Player;

public interface CanvasComponent {
	public int getWidth();
	public int getHeight();
	public boolean isContained();
	public byte getBackground();
	public void setBackground(byte bg);
	public boolean enabled();
	public void setEnabled(boolean enabled);
	public void handleClick(int x, int y, Player player);
	public default CanvasComponent build(CanvasPainter painter, int width, int height, Canvas canvas) {
		return new ConsoleComponent(width, height, (ConsoleRenderer) canvas) {
			@Override
			public void paint(CanvasGraphics g, String context) {
				painter.paint(g, context);
			}
		};
	}
}
