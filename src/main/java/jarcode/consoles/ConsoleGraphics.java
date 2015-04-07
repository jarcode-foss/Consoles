package jarcode.consoles;

import org.bukkit.ChatColor;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

public class ConsoleGraphics {

	private final ConsoleRenderer renderer;
	private final ConsoleComponent component;
	private final Position2D pos;

	private boolean relative = true;

	ConsoleGraphics(ConsoleRenderer renderer, ConsoleComponent component, Position2D pos) {
		this.renderer = renderer;
		this.component = component;
		this.pos = pos;
	}
	public ConsoleGraphics subInstance(ConsoleComponent component, int x, int y) {
		return subInstance(component, new Position2D(x, y));
	}
	public ConsoleGraphics subInstance(ConsoleComponent component, Position2D pos) {
		return new ConsoleGraphics(renderer, component, pos);
	}

	public Position2D containerPosition() {
		return pos.copy();
	}

	public int containerX() {
		return pos.getX();
	}

	public int containerY() {
		return pos.getY();
	}

	public void setRelative(boolean relative) {
		this.relative = relative;
	}

	public boolean isRelative() {
		return relative;
	}

	public final byte drawFormatted(int x, int y, String text) {
		return drawFormatted(x, y, (byte) 32, text);
	}
	@SuppressWarnings("ConstantConditions")
	public final byte drawFormatted(int x, int y, byte inherit, String text) {
		MapFont font = MinecraftFont.Font;
		int at = 0;
		int i = 0;
		char[] arr = text.toCharArray();
		byte color = inherit;
		boolean skipNext = false;
		for (char c : arr) {
			if (i != text.length() - 1 &&
					c == '\u00A7' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(arr[i + 1]) > -1) {
				ChatColor chatColor = ChatColor.getByChar((arr[i + 1] + "").toLowerCase().charAt(0));
				// not all colors properly implemented, so some are just set to white (32)
				// I need to finish sampling these later
				switch (chatColor) {
					case WHITE: color = 32; break;
					case DARK_GRAY: color = 24; break;
					case GRAY: color = 36; break;
					case BLACK: color = 119; break;
					case DARK_BLUE: color = 48; break;
					case DARK_AQUA: color = 48; break;
					case AQUA: color = 20; break;
					case BLUE: color = 20; break;
					case DARK_GREEN: color = 28; break;
					case GREEN: color = 4; break;
					case DARK_PURPLE: color = 32; break;
					case LIGHT_PURPLE: color = 32; break;
					case YELLOW: color = 32; break;
					case RED: color = 16; break;
					case DARK_RED: color = 16; break;
					case GOLD: color = 32; break;
					case RESET: color = 32; break;
				}
				skipNext = true;
			}
			else if(skipNext) {
				skipNext = false;
			}
			else {
				MapFont.CharacterSprite sprite = font.getChar(c);
				for (int k = 0; k < sprite.getWidth(); k++) {
					for (int j = 0; j < sprite.getHeight(); j++) {
						if (sprite.get(j, k)) {
							if (!relative)
								renderer.getPixelBuffer().set(k + at + x, j + y, color, renderer.getPaintContext());
							else
								renderer.getPixelBuffer().set(k + at + x + pos.getX(), j + y + pos.getY(), color,
										renderer.getPaintContext());
						}
					}
				}
				at += sprite.getWidth() + 1;
			}
			i++;
		}
		return color;
	}
	public final void draw(int x, int y, byte color, String text) {
		MapFont font = MinecraftFont.Font;
		int at = 0;
		for (char c : text.toCharArray()) {
			MapFont.CharacterSprite sprite = font.getChar(c);
			for (int k = 0; k < sprite.getWidth(); k++) {
				for (int j = 0; j < sprite.getHeight(); j++) {
					if (sprite.get(j, k)) {
						if (!relative)
							renderer.getPixelBuffer().set(k + at + x, j + y, color, renderer.getPaintContext());
						else
							renderer.getPixelBuffer().set(k + at + x + pos.getX(), j + y + pos.getY(), color,
									renderer.getPaintContext());
					}
				}
			}
			at += sprite.getWidth() + 1;
		}
	}
	public final void draw(int x, int y, byte color) {
		if (!relative)
			renderer.getPixelBuffer().set(x, y, color, renderer.getPaintContext());
		else
			renderer.getPixelBuffer().set(x + pos.getX(), y + pos.getY(), color, renderer.getPaintContext());
	}
	public void drawBackground() {
		if (!component.isContained())
			renderer.drawBackground(pos.getX(), pos.getY(), component.getWidth(), component.getHeight());
	}
	public void drawBackground(int x, int y, int w, int h) {
		if (!relative)
			renderer.drawBackground(x, y, w, h);
		else
			renderer.drawBackground(x + pos.getX(), y + pos.getY(), w, h);
	}
}
