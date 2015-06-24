package ca.jarcode.consoles.internal;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import ca.jarcode.consoles.api.CanvasGraphics;
import ca.jarcode.consoles.util.MonospacedMinecraftFont;
import ca.jarcode.consoles.util.Position2D;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*

A more complex version of the text area component that has indexed lines.

 */

public class IndexedConsoleTextArea extends ConsoleComponent implements WritableComponent {

	protected static final int H_MARGIN = 2;

	protected static final int OFFSET = 20;
	protected static final int MARGIN = 4;

	protected MapFont font = MonospacedMinecraftFont.FONT;
	protected MapFont numberFont = MinecraftFont.Font;
	protected int textHeight = font.getHeight() + 1;
	protected volatile Multimap<Integer, String> stack = createStack();
	protected int maxStackSize;
	private int maxWidth;
	protected byte lastColor = 32;

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
		stack.put(startingLine, "");
		List<String> list = new ArrayList<>();
		section(text, list::add, () -> {}, "\n", false);
		String after = list.stream()
				.skip(startingLine - 1)
				.limit(maxStackSize)
				.collect(Collectors.joining("\n"));
		if (startingLine != 1 && after.isEmpty()) {
			stack.clear();
			return;
		}
		print(after);
	}
	public IndexedConsoleTextArea(int w, int h, ConsoleRenderer renderer) {
		super(w, h, renderer);
		maxStackSize = (h - H_MARGIN) / textHeight;
		maxWidth = w - OFFSET;
	}
	public void print(String text) {
		text = text.replace("\t", "    ");
		if (text.contains("\n")) {
			section(text, this::print, this::advanceLine, "\n", true);
			return;
		}
		if (!text.startsWith("\u00A7"))
			text = ChatColor.RESET + text;
		printContent(text);
	}
	// needlessly complex
	private void removeFirst() {
		Multimap<Integer, String> cached = LinkedHashMultimap.create();
		stack.entries().stream()
				.skip(1)
				.forEach(e -> cached.put(e.getKey(), e.getValue()));
		stack.clear();
		stack = Multimaps.synchronizedMultimap(cached);
	}
	private void removeLast() {
		Multimap<Integer, String> cached = LinkedHashMultimap.create();
		stack.entries().stream()
				.limit(stack.size() - 1 > 0 ? stack.size() - 1 : 0)
				.forEach(e -> cached.put(e.getKey(), e.getValue()));
		stack.clear();
		stack = Multimaps.synchronizedMultimap(cached);
	}
	private void advance() {
		int line = highestLine();
		stack.put(line, "");
	}
	private void nextLine() {
		int line = highestLine();
		stack.put(line + 1, "");
	}
	protected int highestLine() {
		return stack.keySet().stream().max((o1, o2) -> o1 - o2).orElseGet(() -> 1);
	}
	private void appendToCurrentStack(String str) {
		int line = highestLine();
		ArrayList<String> list =
				stack.get(line).stream().collect(Collectors.toCollection(ArrayList::new));
		if (list.size() > 0) {
			list.set(list.size() - 1, list.get(list.size() - 1) + str);
			stack.removeAll(line);
			for (String s : list)
				stack.put(line, s);
		}
		else {
			stack.put(line, str);
		}
	}
	// used to split on newlines and handle accordingly
	protected void section(String text, Consumer<String> handleText, Runnable onSplit, String regex, boolean ignoreEmpty) {
		Matcher matcher = Pattern.compile(regex).matcher(text);
		int first = 0;
		while (matcher.find()) {
			String before = text.substring(first, matcher.start());
			if (!before.isEmpty() || !ignoreEmpty)
				handleText.accept(before);
			onSplit.run();
			first = matcher.end();
		}
		handleText.accept(text.substring(first, text.length()));
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
			removeLast();
		}
	}
	public void println(String text) {
		print(text);
		nextLine();
		if (stack.size() > maxStackSize) {
			removeLast();
		}
	}
	public void advanceLine() {
		nextLine();
		if (stack.size() > maxStackSize) {
			removeLast();
		}
	}
	public void clear() {
		stack.clear();
	}

	@Override
	public void handleClick(int x, int y, Player player) {

	}

	@Override
	public ConsoleMessageListener createListener() {
		return (sender, text) -> {
			println(ChatColor.translateAlternateColorCodes('&', text));
			repaint();
			return "Sent to console";
		};
	}
	protected int currentLine() {
		return stack.size() == 0 ? 0 : stack.size() - 1;
	}
	protected int getHighestReadableLine() {
		Map.Entry<Integer, String> entry =  stack.entries().stream()
				.filter(v -> !v.getValue().isEmpty())
				.max((o1, o2) -> o1.getKey() - o2.getKey()).get();
		return entry == null ? 1 : entry.getKey();
	}
	protected String getLastReadableLine() {
		return stack.values().stream().filter(str -> !str.isEmpty()).reduce((p, curr) -> curr).orElseGet(() -> "");
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
				String str = ChatColor.GRAY.toString() + (entry.getKey() % 1000) + ChatColor.WHITE;
				g.drawFormatted(OFFSET - (numberFont.getWidth(ChatColor.stripColor(str)) + MARGIN),
						(i * textHeight) + H_MARGIN, lastColor, str);
				g.setFont(font);
				k = entry.getKey();
			}
			lastColor = g.drawFormatted(OFFSET, (i * textHeight) + H_MARGIN, lastColor, entry.getValue());
			i++;
		}
	}
}
