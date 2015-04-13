package jarcode.consoles;

import jarcode.consoles.api.CanvasComponent;
import jarcode.consoles.api.CanvasGraphics;
import jarcode.consoles.api.WrappedComponent;
import org.bukkit.ChatColor;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

public class ConsoleGraphics implements CanvasGraphics {

	private final ConsoleRenderer renderer;
	private final ConsoleComponent component;
	private final Position2D pos;

	private boolean relative = true;

	private MapFont font = MinecraftFont.Font;

	ConsoleGraphics(ConsoleRenderer renderer, ConsoleComponent component, Position2D pos) {
		this.renderer = renderer;
		this.component = component;
		this.pos = pos;
	}
	public ConsoleGraphics subInstance(CanvasComponent component, int x, int y) {
		return subInstance(component, new Position2D(x, y));
	}

	@Override
	public byte sample(int x, int y) {
		if (!relative)
			return renderer.getPixelBuffer().get(x, y, renderer.getPaintContext());
		else
			return renderer.getPixelBuffer().get(x + pos.getX(), y + pos.getY(), renderer.getPaintContext());
	}

	public ConsoleGraphics subInstance(CanvasComponent comp, Position2D pos) {
		return new ConsoleGraphics(renderer, comp instanceof WrappedComponent ?
				((WrappedComponent) comp).underlying() : (ConsoleComponent) comp, pos);
	}


	@Override
	public Position2D containerPosition() {
		return pos.copy();
	}

	@Override
	public int containerX() {
		return pos.getX();
	}

	@Override
	public String trim(String text, int len) {
		StringBuilder builder = new StringBuilder();
		char[] arr = text.toCharArray();
		int length = 0;
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			if (c != '\u00A7'
					&& ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) == -1
					|| i == 0 || arr[i - 1] != '\u00A7')) {
				length += font.getChar(c).getWidth() + (i == arr.length - 1 ? 0 : 1);
				if (length > len) {
					break;
				}
				else builder.append(c);
			} else builder.append(c);
		}
		return builder.toString();
	}
	@Override
	public int getWidth() {
		return component.getWidth();
	}

	@Override
	public int getHeight() {
		return component.getWidth();
	}

	@Override
	public int containerY() {
		return pos.getY();
	}

	@Override
	public void setRelative(boolean relative) {
		this.relative = relative;
	}

	@Override
	public CanvasComponent getComponent() {
		return component;
	}

	public boolean isRelative() {
		return relative;
	}

	public final byte drawFormatted(int x, int y, String text) {
		return drawFormatted(x, y, (byte) 32, text);
	}
	public final byte drawFormatted(int x, int y, byte inherit, String text) {
		return drawFormatted(x, y, inherit, text, null);
	}
	@SuppressWarnings("ConstantConditions")
	public final byte drawFormatted(int x, int y, byte inherit, String text, CharacterModifier modifier) {
		int at = 0;
		int i = 0;
		int charIndex = 0;
		char[] arr = text.toCharArray();
		byte color = inherit;
		boolean skipNext = false;
		for (char c : arr) {
			if (i != text.length() - 1 &&
					c == '\u00A7' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(arr[i + 1]) > -1) {
				ChatColor chatColor = ChatColor.getByChar((arr[i + 1] + "").toLowerCase().charAt(0));
				switch (chatColor) {
					case WHITE: color = 32; break;
					case DARK_GRAY: color = 24; break;
					case GRAY: color = 36; break;
					case BLACK: color = 119; break;
					case DARK_BLUE: color = 48; break;
					case DARK_AQUA: color = 127; break;
					case AQUA: color = 125; break;
					case BLUE: color = 70; break;
					case DARK_GREEN: color = 28; break;
					case GREEN: color = 6; break;
					case DARK_PURPLE: color = 67; break;
					case LIGHT_PURPLE: color = 66; break;
					case YELLOW: color = 74; break;
					case RED: color = 114; break;
					case DARK_RED: color = 115; break;
					case GOLD: color = 61; break;
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
				if (modifier != null)
					modifier.paint(charIndex, c, sprite, at + x, y);
				at += sprite.getWidth() + 1;
				charIndex++;
			}
			i++;
		}
		return color;
	}

	@Override
	public void setFont(MapFont font) {
		this.font = font;
	}

	public final void draw(int x, int y, byte color, String text) {
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

	@FunctionalInterface
	public static interface CharacterModifier {
		public void paint(int index, char c, MapFont.CharacterSprite sprite, int px, int py);
	}
}
