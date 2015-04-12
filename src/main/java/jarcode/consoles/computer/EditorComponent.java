package jarcode.consoles.computer;

import com.google.common.base.Joiner;
import jarcode.consoles.*;
import jarcode.consoles.api.CanvasGraphics;
import org.bukkit.ChatColor;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EditorComponent extends ConsoleComponent implements WritableComponent {

	private static final int OFFSET = 20;

	private MonospacedMinecraftFont font = MonospacedMinecraftFont.FONT;
	private MapFont numberFont = MinecraftFont.Font;
	private int textHeight = font.getHeight() + 1;
	private String text = "A bunch of default text that you should get used to seeing on a regular basis.\nThis is a new top\nThis is another new top.\nWow, I can create an infinite amount of new lines! I wonder what happens if I just keep writing... does it bug out? Maybe... just maybe, but I'm running out of things to write.";
	private int top = 1;
	private int column = 1;
	private List<String> stack = new CopyOnWriteArrayList<>();
	private int maxStackSize;
	private int maxWidth;
	private byte lastColor = 32;

	{
		stack.add("");
	}
	public void setLineNumberFont(MapFont font) {
		this.numberFont = font;
	}
	public static EditorComponent createOver(ConsoleRenderer renderer) {
		return new EditorComponent(renderer.getWidth() - 4, renderer.getHeight() - 4, renderer);
	}

	public void placeOver(ConsoleRenderer renderer) {
		renderer.putComponent(new Position2D(2, 2), this);
	}
	public EditorComponent(int w, int h, ConsoleRenderer renderer) {
		super(w, h, renderer);
		maxStackSize = h / textHeight;
		maxWidth = w - OFFSET;
	}
	public void setTop(int top) {
		this.top = top;
	}
	private void appendToStack(String text) {
		text = text.replace("\t", "    ");
		if (text.contains("\n")) {
			section(text, this::appendToStack, this::advanceLine, "\n");
			return;
		}
		if (!text.startsWith("\u00A7"))
			text = ChatColor.RESET + text;
		printContent(text);
	}
	public void renderView() {
		stack.clear();
		stack.add("");
		String[] split = text.split("\n");
		String after = Arrays.asList(split).stream().skip(top - 1).limit(maxStackSize).collect(Collectors.joining("\n"));
		appendToStack(after);
	}
	// used to split on newlines and handle accordingly
	protected void section(String text, Consumer<String> handleText, Runnable onSplit, String regex) {
		int count = 0;
		Matcher matcher = Pattern.compile(regex).matcher(text);
		int first = 0;
		while (matcher.find()) {
			String before = text.substring(first, matcher.start());
			count++;
			if (!before.isEmpty())
				handleText.accept(before);
			onSplit.run();
			first = matcher.end();
		}
		if (count == 0 || (first < text.length() && first != -1)) {
			handleText.accept(text.substring(first, text.length()));
		}
	}
	private void printContent(String text) {
		text = ManagedConsole.removeUnsupportedCharacters(text);
		String stripped = ChatColor.stripColor(text + getLastLine());
		if (font.getWidth(stripped) > maxWidth) {
			String[] split = text.split(" ");
			List<String> list = new ArrayList<>();
			int index = 0;
			for (String s : split) {
				list.add(s);
				String comb = ChatColor.stripColor(Joiner.on(" ").join(list) + getLastLine());
				if (font.getWidth(comb) <= maxWidth) {
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
						if (arr[t] != '\u00A7'
								&& ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(arr[t]) == -1
								|| t == 0 || arr[t - 1] != '\u00A7')) {
							check.append(arr[t]);
							String bare = check.toString();
							if (font.getWidth(bare) > maxWidth) {
								break;
							} else builder.append(arr[t]);
						}
						else builder.append(arr[t]);
					};
					// add sectioned
					printContent(builder.toString());
					stack.add("");
					// add other portion
					printContent(text.substring(t, text.length()));
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
		appendToStack(text);
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
		g.setFont(font);
		g.drawBackground();
		for (int t = 0; t < stack.size(); t++) {
			lastColor = g.drawFormatted(OFFSET, (t * textHeight), lastColor, stack.get(t));
		}
	}
}
