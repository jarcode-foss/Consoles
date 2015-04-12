package jarcode.consoles;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import jarcode.consoles.api.CanvasGraphics;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IndexedConsoleTextArea extends ConsoleComponent implements WritableComponent {

	private static final int OFFSET = 20;
	private static final int MARGIN = 4;

	private MapFont font = MinecraftFont.Font;
	private MapFont numberFont = MinecraftFont.Font;
	private int textHeight = font.getHeight() + 1;
	private volatile Multimap<Integer, String> stack = createStack();
	private int maxStackSize;
	private int maxWidth;
	private byte lastColor = 32;

	private static Multimap<Integer, String> createStack() {
		return Multimaps.synchronizedMultimap(LinkedHashMultimap.create());
	}

	{
		stack.put(1, "");
	}
	public void setFont(MapFont font) {
		this.font = font;
	}
	public void setNumberFont(MapFont font) {
		this.numberFont = font;
	}
	public static IndexedConsoleTextArea createOver(ConsoleRenderer renderer) {
		return new IndexedConsoleTextArea(renderer.getWidth() - 4, renderer.getHeight() - 4, renderer);
	}

	public void placeOver(ConsoleRenderer renderer) {
		renderer.putComponent(new Position2D(2, 2), this);
	}
	public void setText(String text) {
		stack.clear();
		print(text);
	}
	public void setText(String text, int startingLine) {
		stack.clear();
		String after = Arrays.asList(text.split("\n")).stream()
				.skip(startingLine - 1)
				.limit(maxStackSize)
				.collect(Collectors.joining("\n"));
		print(after);
	}
	public IndexedConsoleTextArea(int w, int h, ConsoleRenderer renderer) {
		super(w, h, renderer);
		maxStackSize = h / textHeight;
		maxWidth = w - OFFSET;
	}
	public void print(String text) {
		text = text.replace("\t", "    ");
		if (text.contains("\n")) {
			section(text, this::print, this::advanceLine, "\n");
			return;
		}
		if (!text.startsWith("\u00A7"))
			text = ChatColor.RESET + text;
		printContent(text);
	}
	// needlessly complex
	private void removeFirst() {
		stack = Multimaps.synchronizedMultimap(Multimaps.forMap(
				stack.entries().stream()
				.skip(1)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, o2) -> o, LinkedHashMap::new))
		));
	}
	private void advance() {
		int line = highestLine();
		stack.put(line, "");
	}
	private void nextLine() {
		int line = highestLine();
		stack.put(line + 1, "");
	}
	private int highestLine() {
		return stack.keySet().stream().max((o1, o2) -> o1 - o2).orElseGet(() -> 1);
	}
	private void appendToCurrentStack(String str) {
		int line = highestLine();
		ArrayList<String> list =
				stack.get(line).stream().collect(Collectors.toCollection(ArrayList::new));
		if (list.size() > 0) {
			list.set(list.size() - 1, list.get(list.size() - 1) + str);
			stack.removeAll(line);
			stack.putAll(line, list);
		}
		else {
			stack.put(line, str);
		}
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
					}
					// add sectioned
					printContent(builder.toString());
					advance();
					// add other portion
					printContent(text.substring(t, text.length()));
				}
				// line isn't empty, try to fit into new line
				else {
					advance();
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
				appendToCurrentStack(str);

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
			appendToCurrentStack(text);
		}
		while (stack.size() > maxStackSize) {
			removeFirst();
		}
	}
	public void println(String text) {
		print(text);
		nextLine();
		if (stack.size() > maxStackSize) {
			removeFirst();
		}
	}
	public void advanceLine() {
		nextLine();
		if (stack.size() > maxStackSize) {
			removeFirst();
		}
	}
	public void clear() {
		stack.clear();
	}

	@Override
	public void handleClick(int x, int y, Player player) {

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
		return stack.values().stream().reduce((p, curr) -> curr).orElseGet(() -> "");
	}
	@Override
	public void paint(CanvasGraphics g, String context) {
		g.setFont(font);
		g.drawBackground();
		int i = 0;
		int k = -1;
		for (Map.Entry<Integer, String> entry : stack.entries()) {
			if (k != entry.getKey()) {
				g.setFont(numberFont);
				String str = ChatColor.GRAY.toString() + entry.getKey() + ChatColor.WHITE;
				g.drawFormatted(OFFSET - (numberFont.getWidth(ChatColor.stripColor(str)) + MARGIN), (i * textHeight), lastColor, str);
				g.setFont(font);
				k = entry.getKey();
			}
			lastColor = g.drawFormatted(OFFSET, (i * textHeight), lastColor, entry.getValue());
			i++;
		}
	}
}
