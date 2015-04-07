package jarcode.consoles.api;

import jarcode.consoles.ConsoleComponent;
import org.bukkit.entity.Player;

//TODO: finish!
public class CanvasDialog implements CanvasComponent, CanvasPainter, WrappedComponent {
	@Override
	public int getWidth() {
		return 0;
	}

	@Override
	public int getHeight() {
		return 0;
	}

	@Override
	public boolean isContained() {
		return false;
	}

	@Override
	public byte getBackground() {
		return 0;
	}

	@Override
	public void setBackground(byte bg) {

	}

	@Override
	public boolean enabled() {
		return false;
	}

	@Override
	public void setEnabled(boolean enabled) {

	}

	@Override
	public void handleClick(int x, int y, Player player) {

	}

	@Override
	public ConsoleComponent underlying() {
		return null;
	}

	@Override
	public void paint(CanvasGraphics g, String context) {

	}

	public CanvasDialog
}
