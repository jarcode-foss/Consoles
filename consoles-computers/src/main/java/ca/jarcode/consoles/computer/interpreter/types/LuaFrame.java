package ca.jarcode.consoles.computer.interpreter.types;

import ca.jarcode.consoles.api.CanvasGraphics;
import ca.jarcode.consoles.CColor;
import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.manual.Arg;
import ca.jarcode.consoles.computer.manual.FunctionManual;
import ca.jarcode.consoles.computer.manual.TypeManual;
import org.bukkit.ChatColor;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@TypeManual(
		value = "Represents a single frame that can be drawn to the screen. Contains " +
		"methods for drawing various content to the frame, and can be used with LuaBuffer to " +
		"update a session.",
		usage = "-- Creates a new frame\n" +
				"local screen = screenFrame()\n" +
				"-- Draw some text\n" +
				"screen:write(24, 16, \"Hello World!\")\n" +
				"-- Update a LuaBuffer with this frame\n" +
				"buffer:update(screen:id())")
@SuppressWarnings("unused")
public class LuaFrame {

	private static final MapFont FONT = MinecraftFont.Font;

	public static byte convert(int c) {
		if (c >= 0 && c <= 127) {
			return (byte) (int) c;
		}
		else if (c > 127 && c <= 143) {
			return (byte) (-128 + (c - 127));
		}
		else return (byte) 0;
	}

	protected List<Consumer<CanvasGraphics>> operations = new ArrayList<>();
	private Computer computer;
	private int id;
	private Runnable remove;
	private boolean removed = false;

	public LuaFrame(int id, Computer computer, Runnable remove) {
		this.computer = computer;
		this.id = id;
		this.remove = remove;
	}
	@FunctionManual("Removes the frame and cleans up resources. This function is called automatically when " +
			"using the frame to update a screen buffer.")
	public void remove() {
		if (removed) return;
		removed = true;
		remove.run();
	}

	@FunctionManual("Returns the ID of this fame")
	public int id() {
		if (removed) return -1;
		return id;
	}

	@FunctionManual("Sets the X,Y coordinate to the specified map color.")
	public void set(
			@Arg(name = "x", info = "X coordinate") Integer x,
			@Arg(name = "y", info = "Y coordinate") Integer y,
			@Arg(name = "c", info = "the minecraft map color to use") Integer c) {
		if (removed) return;
		if (x >= 0 && y >= 0 && getHeight() > y && getWidth() > x)
			operations.add((g) -> g.draw(x, y, convert(c)));
	}
	@FunctionManual("Returns the length, in pixels, of the text passed through this function. Ignores " +
			"color formatting.")
	public int len(
			@Arg(name = "text", info = "the text to parse") String text) {
		if (removed) return -1;
		text = ChatColor.translateAlternateColorCodes('&', text);
		return FONT.getWidth(CColor.strip(text).replace("\u00A7", "&"));
	}
	@FunctionManual("Draws the specified text at the given X,Y coordinates. Color codes are supported.")
	public void write(
			@Arg(name = "x", info = "X coordinate") Integer x,
			@Arg(name = "y", info = "Y coordinate") Integer y,
			@Arg(name = "text", info = "the text to print on the frame")String text) {
		if (removed) return;
		text = ChatColor.translateAlternateColorCodes('&', text);
		text = text.replace("\n", "");
		final String t = text;
		operations.add((g) -> g.drawFormatted(x, y, t));
	}
	@FunctionManual("Draws a filled box in the given area.")
	public void box(
			@Arg(name = "x", info = "X coordinate")  Integer x,
			@Arg(name = "y", info = "Y coordinate") Integer y,
			@Arg(name = "w", info = "box width") Integer w,
			@Arg(name = "h", info = "box height") Integer h,
			@Arg(name = "c", info = "box color") Integer c) {
		if (removed) return;
		byte converted = convert(c);
		operations.add((g) -> {
			for (int t = x; t < x + w; t++) {
				for (int j = y; j < y + h; j++) {
					g.draw(x, y, converted);
				}
			}
		});
	}
	@FunctionManual("Fills the entire frame with the given color")
	public void fill(
			@Arg(name = "c", info = "the color to fill with") Integer c) {
		if (removed) return;
		byte converted = convert(c);
		operations.add((g) -> {
			for (int t = 0; t < g.getWidth(); t++) {
				for (int j = 0; j < g.getHeight(); j++) {
					g.draw(t, j, converted);
				}
			}
		});
	}
	@FunctionManual("Returns the width of the frame")
	public int getWidth() {
		if (removed) return -1;
		return computer.getViewWidth();
	}
	@FunctionManual("Returns the height of the frame")
	public int getHeight() {
		if (removed) return -1;
		return computer.getViewHeight();
	}
}
