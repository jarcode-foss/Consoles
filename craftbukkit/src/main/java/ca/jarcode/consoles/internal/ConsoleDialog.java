package ca.jarcode.consoles.internal;

import ca.jarcode.consoles.api.CanvasComponent;
import ca.jarcode.consoles.api.CanvasGraphics;
import ca.jarcode.consoles.util.Position2D;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.map.MinecraftFont;

import java.util.AbstractMap;
import java.util.Map;

/*

A dialog that can be instantiated with components and text, sizing itself accordingly.

 */
public class ConsoleDialog extends ConsoleContainer {

	private static final int MARGIN = 5;

	// Safe way of using dialogs
	public static Map.Entry<Position2D, ConsoleDialog> create(ConsoleRenderer renderer, String text, ConsoleComponent... children) {
		int w = totalWidthOf(children, MARGIN) + 6;
		int f = largestTextWidth(text) + 6;
		if (f > w)
			w = f;
		int h = maxHeightOf(children) + 14 + (text.split("\n").length * (MinecraftFont.Font.getHeight() + 1));
		int x = (renderer.getWidth() / 2) - (w / 2);
		int y = (renderer.getHeight() / 2) - (h / 2);
		ConsoleDialog dialog = new ConsoleDialog(w, h, renderer);
		dialog.setEnabled(true);
		dialog.setText(text);
		for (ConsoleComponent component : children) {
			dialog.add(component);
		}
		return new AbstractMap.SimpleEntry<>(new Position2D(x, y), dialog);
	}

	private static int largestTextWidth(String text) {
		int l = 0;
		for (String str : text.split("\n")) {
			int f = MinecraftFont.Font.getWidth(ChatColor.stripColor(str)) + 6;
			if (f > l)
				l = f;
		}
		return l;
	}

	public static ConsoleDialog show(ConsoleRenderer renderer, String text, ConsoleComponent... children) {
		Map.Entry<Position2D, ConsoleDialog> entry = create(renderer, text, children);
		renderer.putComponent(entry.getKey(), entry.getValue());
		return entry.getValue();
	}

	private String text;

	// manual dialog creation. Don't use this outside of component code.
	public ConsoleDialog(int w, int h, ConsoleRenderer renderer) {
		super(w, h, renderer, false);
		setBackground((byte) 20);
	}

	public void setText(String text) {
		this.text = text;
	}
	@Override
	public void onClick(int x, int y, Player player) {}

	@Override
	public Position2D getUnderlingComponentCoordinates(CanvasComponent component) {
		int w = totalContainedWidth(MARGIN);
		int at = 0;
		for (ConsoleComponent comp : contained) {
			if (comp == component)
				return new Position2D(at + (getWidth() / 2) - (w / 2), getHeight() - (maxContainedHeight() + 3));
			at += comp.getWidth() + MARGIN;
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
		int count = 0;
		byte color = 119;
		for (String str : text.split("\n")) {
			int w = MinecraftFont.Font.getWidth(ChatColor.stripColor(str));
			color = g.drawFormatted(x + (getWidth() / 2) - (w / 2),
					y + 8 + (count * (MinecraftFont.Font.getHeight() + 1)), color, str);
			count++;
		}

		int wd = totalContainedWidth(MARGIN);
		int at = 0;
		for (ConsoleComponent component : contained) {
			component.paint(g.subInstance(component,
					new Position2D(x + at + (getWidth() / 2) - (wd / 2), y + getHeight() - (maxContainedHeight() + 3))),
					context);
			at += component.getWidth() + MARGIN;
		}
	}
}
