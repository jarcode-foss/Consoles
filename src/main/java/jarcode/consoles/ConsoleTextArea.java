package jarcode.consoles;

import jarcode.consoles.api.CanvasGraphics;
import org.bukkit.ChatColor;
import org.bukkit.map.MinecraftFont;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleTextArea extends ConsoleComponent implements WritableComponent {

	private MinecraftFont font = MinecraftFont.Font;
	private int textHeight = font.getHeight() + 1;
	private List<String> stack = new ArrayList<>();
	private int maxStackSize;
	private int maxWidth;
	private byte lastColor = 32;

	{
		stack.add("");
	}

	public static ConsoleTextArea createOver(ConsoleRenderer renderer) {
		return new ConsoleTextArea(renderer.getWidth() - 4, renderer.getHeight() - 4, renderer);
	}
	public void placeOver(ConsoleRenderer renderer) {
		renderer.putComponent(new Position2D(2, 2), this);
	}
	public ConsoleTextArea(int w, int h, ConsoleRenderer renderer) {
		super(w, h, renderer);
		maxStackSize = h / textHeight;
		maxWidth = w;
	}
	public void print(String text) {
		text = text.replace("\t", "    ");
		if (text.contains("\n")) {
			List<String> list = section(text);
			for (int t = 0; t < list.size(); t++) {
				print(list.get(t));
				if (t != list.size() - 1)
					advanceLine();
			}
			return;
		}
		if (!text.startsWith("\u00A7"))
			text = ChatColor.RESET + text;
		printContent(text);
	}
	protected List<String> section(String text) {
		List<String> list = new ArrayList<>();
		Matcher matcher = Pattern.compile("\\n").matcher(text);
		int first = 0;
		while (matcher.find()) {
			list.add(text.substring(first, matcher.start()));
			first = matcher.end();
		}
		if (first < list.size() - 1 && first != -1) {
			list.add(text.substring(first, text.length()));
		}
		else list.add("");
		return list;
	}
	private void printContent(String text) {
		text = ManagedConsole.removeUnsupportedCharacters(text);
		String stripped = ChatColor.stripColor(text + getLastLine());
		if (font.getWidth(stripped) > maxWidth) {
			String[] split = text.split(" ");
			StringBuilder combined = new StringBuilder();
			int index = 0;
			for (String s : split) {
				combined.append(s);
				if (font.getWidth(ChatColor.stripColor(combined.toString() + getLastLine())) <= maxWidth) {
					index++;
				}
				else break;
			}
			// can't fit
			if (index == 0) {
				// line is empty, can't fit
				if (ChatColor.stripColor(getLastLine()).isEmpty()) {
					StringBuilder builder = new StringBuilder();
					StringBuilder check = new StringBuilder();
					char[] arr = text.toCharArray();
					int t;
					for (t = 0; t < text.length(); t++) {
						check.append(arr[t]);
						if (font.getWidth(ChatColor.stripColor(check.toString())) > maxWidth) {
							break;
						}
						else builder.append(arr[t]);
					}
					// add sectioned
					printContent(builder.toString());
					stack.add("");
					// add other portion
					printContent(text.substring(t - 1, text.length()));
				}
				// line isn't empty, try to fit into new line
				else {
					stack.add("");
					printContent(text);
				}
			}
			// partial fit
			else {
				StringBuilder builder = new StringBuilder();
				for (int t = 0; t < index; t++) {
					builder.append(split[t]);
					if (t != index - 1)
						builder.append(" ");
				}
				//fit text
				String str = builder.toString();
				stack.set(currentLine(), getLastLine() + str);

				builder = new StringBuilder();
				for (int t = index; t < split.length; t++) {
					builder.append(split[t]);
					if (t != split.length - 1)
						builder.append(" ");
				}
				//remaining
				printContent(builder.toString());
			}
		}
		// fit
		else {
			stack.set(currentLine(), getLastLine() + text);
		}
		while (stack.size() > maxStackSize) {
			stack.remove(0);
		}
	}
	public void println(String text) {
		print(text);
		stack.add("");
		if (stack.size() > maxStackSize) {
			stack.remove(0);
		}
	}
	public void advanceLine() {
		stack.add("");
		if (stack.size() > maxStackSize) {
			stack.remove(0);
		}
	}
	public void clear() {
		stack.clear();
		stack.add("");
	}
	@Override
	public ConsoleListener createListener() {
		return (sender, text) -> {
			println(ChatColor.translateAlternateColorCodes('&', text));
			repaint();
			return "Sent to console";
		};
	}
	protected int currentLine() {
		return stack.size() == 0 ? 0 : stack.size() - 1;
	}
	protected String getLastLine() {
		return stack.size() == 0 ? "" : stack.get(currentLine());
	}
	@Override
	public void paint(CanvasGraphics g, String context) {
		g.drawBackground();
		for (int t = 0; t < stack.size(); t++) {
			lastColor = g.drawFormatted(0, (t * textHeight), lastColor, stack.get(t));
		}
	}
}
