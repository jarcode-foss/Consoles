package jarcode.consoles.computer;

import jarcode.consoles.internal.ConsoleComponent;
import jarcode.consoles.internal.ConsoleRenderer;
import jarcode.consoles.api.CanvasGraphics;

/*

Status bar (the top text thingy) for a computer.

 */
public class StatusBar extends ConsoleComponent {

	static final int HEIGHT = 16;
	private String text = "";

	public StatusBar(ConsoleRenderer renderer) {
		super(renderer.getWidth() - 4, HEIGHT, renderer);
	}

	public synchronized void setText(String text) {
		this.text = text;
	}

	@Override
	public void paint(CanvasGraphics g, String context) {
		g.drawBackground();
		synchronized (this) {
			if (text != null)
				g.drawFormatted(2, 3, g.trim(text, getWidth() - 6));
		}
		for (int x = 0; x < g.getWidth(); x++) {
			g.draw(x, getHeight() - 2, (byte) 44);
			g.draw(x, getHeight() - 1, (byte) 47);
		}
	}
}
