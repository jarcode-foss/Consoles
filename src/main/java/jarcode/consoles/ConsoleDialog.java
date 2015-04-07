package jarcode.consoles;

import jarcode.consoles.api.CanvasComponent;
import jarcode.consoles.api.CanvasGraphics;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.map.MinecraftFont;

public class ConsoleDialog extends ConsoleContainer {

	private static final int MARGIN = 5;

	// Safe way of using dialogs
	public static ConsoleDialog show(ConsoleRenderer renderer, String text, ConsoleComponent... children) {
		int w = totalWidthOf(children, MARGIN) + 6;
		int f = MinecraftFont.Font.getWidth(ChatColor.stripColor(text)) + 6;
		if (f > w)
			w = f;
		int h = maxHeightOf(children) + 21;
		int x = (renderer.getWidth() / 2) - (w / 2);
		int y = (renderer.getHeight() / 2) - (h / 2);
		ConsoleDialog dialog = new ConsoleDialog(w, h, renderer);
		dialog.setEnabled(true);
		dialog.setText(text);
		for (ConsoleComponent component : children) {
			dialog.add(component);
		}
		renderer.putComponent(new Position2D(x, y), dialog);
		return dialog;
	}

	private String text;

	// manual dialog creation. Don't use this outside of component code.
	public ConsoleDialog(int w, int h, ConsoleRenderer renderer) {
		super(w, h, renderer, false);
		setBackground((byte) 20);
	}

	public void setText(String text) {
		if (!text.startsWith("\u00A7"))
			this.text = ChatColor.BLACK + text;
		else
			this.text = text;
	}
	@Override
	public void onClick(int x, int y, Player player) {}

	@Override
	public void add(CanvasComponent component) {}

	@Override
	public Position2D getUnderlingComponentCoordinates(CanvasComponent component) {
		int w = totalContainedWidth(MARGIN);
		int at = 0;
		for (ConsoleComponent comp : contained) {
			if (comp == component)
				return new Position2D(at + (getWidth() / 2) - (w / 2), getHeight() - (maxContainedHeight() + 3));
			at += component.getWidth() + MARGIN;
		}
		return null;
	}
	@Override
	public void paint(CanvasGraphics g, String context) {
		g.drawBackground();
		g.setRelative(false);
		int x = g.containerX();
		int y = g.containerY();
		for (int i = x; i < x + getWidth(); i++) {
			g.draw(i, y, (byte) 48);
			g.draw(i, y + 1, (byte) 48);
			g.draw(i, y + getHeight() - 1, (byte) 48);
			g.draw(i, y + getHeight() - 2, (byte) 48);
			if (i <= x + 1 || i >= x + getWidth() - 2) for (int j = y; j < y + getHeight(); j++) {
				g.draw(i, j, (byte) 48);
			}
		}
		int w = MinecraftFont.Font.getWidth(ChatColor.stripColor(text));
		g.drawFormatted(x + (getWidth() / 2) - (w / 2), y + 8, text);
		for (int i = x + 4; i < getWidth() - 8; i++) {
			g.draw(i, y + 17, (byte) 24);
		}
		w = totalContainedWidth(MARGIN);
		int at = 0;
		for (ConsoleComponent component : contained) {
			component.paint(g.subInstance(component,
					new Position2D(x + at + (getWidth() / 2) - (w / 2), y + getHeight() - (maxContainedHeight() + 3))),
					context);
			at += component.getWidth() + MARGIN;
		}
	}
}
