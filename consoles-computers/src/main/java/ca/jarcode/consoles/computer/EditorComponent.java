package ca.jarcode.consoles.computer;

import ca.jarcode.consoles.api.CanvasGraphics;
import ca.jarcode.consoles.computer.filesystem.FSFile;
import ca.jarcode.consoles.internal.ConsoleGraphics;
import ca.jarcode.consoles.internal.IndexedConsoleTextArea;
import ca.jarcode.consoles.internal.InputComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/*

The bulk of the edit program, contains almost everything as an extension
of the indexed text area, all as a component.

This class contains some of the most confusing code I have ever written.
I am terribly sorry for anyone else who has to work with this.

 */
public class EditorComponent extends IndexedConsoleTextArea implements InputComponent {

	// The editable content of this editor. Used to rebuild the component.
	private String content;

	// the processed content, which is re-parsed every time the original content changes.
	private String processedContent;

	// whether to process (color) the editor's contents
	private boolean processed = true;

	// The top viewable line
	private int top = 1;
	// cursor color (text)
	private byte cursorColorPrimary = (byte) 118;
	// secondary cursor color (bg)
	private byte cursorColorSecondary = (byte) 32;

	// The row that is currently selected (cursor)
	private volatile int row = 1;
	// The index of the character on the current row that is selected (cursor)
	private volatile int character = 1;

	// The computer this editor belongs to
	private Computer computer;
	// The file being edited
	private FSFile file;
	// The screen session this is operating in
	private int tty;

	private static final char[] COLOR_SPLITTERS = " ,:=()~![{}]<>;".toCharArray();
	private static final char[] NUMBERS = "0987654321".toCharArray();
	private static final String[] KEYWORDS = { "and", "end", "in", "repeat",
			"break", "false", "local", "return", "do", "for", "nil", "then",
			"else", "function", "not", "true", "elseif", "if", "or", "until",
			"while" };
	private static final String[] HIGHLIGHTED_MEMBERS = { "require" };

	private static final String[] OPERATORS = { "+", "-", "*", "/", "%", "^",
			"#", "==", "~=", "<=", ">=", "<", ">", "=", "(", ")", "{", "}",
			"[", "]", ";", ":", ",", ".", "..", "..."};

	private static final char[] OPENING_DELIMETERS = {'(', ')', '\n', ' '};

	private static final List<Function<String, int[][]>> processors = new ArrayList<>();
	private static final List<String> formatters = new ArrayList<>();

	// functions must produce ranges ordered least -> greatest
	private static void reg(Function<String, int[][]> func, String formatter) {
		processors.add(func);
		formatters.add(formatter);
	}

	static {
		reg(EditorComponent::findCommentRanges, ChatColor.GRAY.toString());
		reg(EditorComponent::findStringRanges, ChatColor.GREEN.toString());
	}

	private static int[][] findStringRanges(String content) {
		List<int[]> ranges = new ArrayList<>();
		char[] arr = content.toCharArray();
		int first = -1;
		char strType = '\0';
		for (int t = 0; t < content.length(); t++) {
			if (first == -1) {
				if (arr[t] == '"' || arr[t] == '\'') {
					first = t;
					strType = arr[t];
				}
			}
			else if ((arr[t] == '"' || arr[t] == '\'') && strType == arr[t] && arr[t - 1] != '\\') {
				ranges.add(new int[] { first, t });
				first = -1;
			}
		}
		if (first != -1)
			ranges.add(new int[] { first, content.length() - 1 });

		return ranges.toArray(new int[ranges.size()][]);
	}
	private static int[][] findCommentRanges(String content) {
		List<int[]> ret = new ArrayList<>();
		int d;
		char[] arr = content.toCharArray();
		for (int t = 1; t < content.length(); t++) {
			if (arr[t] == '-' && arr[t - 1] == '-') {
				if (t < content.length() - 2 && arr[t + 1] == '[' && arr[t + 2] == '[') {
					d = content.indexOf("--]]", t - 1);
					if (d == -1) {
						ret.add(new int[] { t - 1, content.length() - 1 });
						break;
					}
					ret.add(new int[] { t - 1, d + 3 });
					t = d + 4;
				}
				else {
					d = content.indexOf('\n', t - 1);
					if (d == - 1) {
						ret.add(new int[] { t - 1, content.length() - 1 });
						break;
					}
					ret.add(new int[] { t - 1, d });
					t = d + 1;
				}
			}
		}
		int[][] ints = new int[ret.size()][];
		for (int t = 0; t < ret.size(); t++)
			ints[t] = ret.get(t);
		return ints;
	}
	private static String process(String content) {
		List<int[]> stack;
		int size;
		{
			int[][][] ranges = new int[processors.size()][][];
			for (int t = 0; t < ranges.length; t++)
				ranges[t] = processors.get(t).apply(content);
			for (int t = 0; t < ranges.length; t++) {
				for (int d = 0; d < ranges.length; d++) {
					if (t != d) {
						if (t > d)
							handleOverlaps(ranges[d], ranges[t]);
						else if (d > t)
							handleOverlaps(ranges[t], ranges[d]);
					}
				}
			}
			int fsize = 0;
			for (int[][] range : ranges) {
				fsize += range.length;
			}
			int[][] fstack = new int[fsize][];
			int counter = 0;
			for (int t = 0; t < ranges.length; t++) {
				for (int[] set : ranges[t]) {
					if (set != null) {
						fstack[counter] = new int[] { set[0], set[1], t };
						counter++;
					}
				}
			}
			size = counter;
			stack = Arrays.asList(fstack);
		}
		Collections.sort(stack, (first, second) -> first[0] - second[0]);

		StringBuilder builder = new StringBuilder();
		int index = 0;
		for (int t = 0; t < size; t++) {
			int[] range = stack.get(t);
			if (index < content.length() && index < range[0]) {
				String sub = content.substring(index, range[0]);
				builder.append(sub);
			}
			builder.append(formatters.get(range[2]));
			builder.append(content.substring(range[0], range[1] + 1));
			builder.append(ChatColor.RESET);
			index = range[1] + 1;
		}
		if (index < content.length())
			builder.append(content.substring(index, content.length()));
		return builder.toString();
	}
	private static void handleOverlaps(int[][] primary, int[][] secondary) {
		if (secondary.length == 0 || primary.length == 0) return;
		for (int t = 0; t < primary.length; t++) {
			if (primary[t] == null)
				continue;
			// optimisation, skip all ranges before first colliding index
			if (primary[t][1] < secondary[0][0]) {
				t++;
				continue;
			}
			// optimisation, skip all ranges after final colliding index
			if (primary[t][0] > secondary[secondary.length - 1][1])
				break;
			for (int d = 0; d < secondary.length; d++) {
				if (secondary[d] == null)
					continue;
				if (overlap(primary[t], secondary[d])) {
					if (!shrink(secondary[d], primary[t]))
						secondary[d] = null;
				}
			}
		}
	}
	public static boolean overlap(int[] collision, int[] context) {
		return inside(collision, context[0], context[1])
				|| inside(context, collision[0], collision[1]);
	}
	public static boolean shrink(int[] context, int[] reference) {
		// context is inside the reference range
		if (inside(reference, context[0]) && inside(reference, context[1]))
			return false;
			// shift to the right
		else if (inside(reference, context[0])) {
			if (reference[1] + 1 < context[1])
				context[0] = reference[1] + 1;
				// if context was shrunk to zero length
			else return false;
		}
		// shift to the left
		else if (inside(reference, context[1])) {
			if (reference[0] - 1 > context[0])
				context[1] = reference[0] - 1;
				// if context was shrunk to zero length
			else return false;
		}
		return true;
	}
	public static boolean inside(int[] range, int... indexes) {
		for (int index : indexes) {
			if (index > range[0] && index < range[1])
				return true;
		}
		return false;
	}

	public EditorComponent(int w, int h, Computer computer, FSFile file, int tty) {
		super(w, h, computer.getConsole());
		this.computer = computer;
		this.tty = tty;
		this.file = file;
	}

	// unsafe
	@Override
	public void println(String text) {
		super.println(text);
	}

	// unsafe
	@Override
	public void print(String text) {
		super.print(text);
	}

	public void setCursor(int row, int character) {
		this.row = row < 1 ? this.row : row;
		this.character = character < 1 ? this.character : character;
	}
	public void setView(int top) {
		this.top = top;
		if (row < top)
			row = top;
	}
	public void process() {
		if (processed) {
			processedContent = process(content);
		}
	}
	public void rebuild() {
		setContent(content, false);
	}
	public void changed() {
		setContent(content);
	}
	private int[] resize(int[] arr) {
		int[] tmp = new int[arr.length << 2];
		System.arraycopy(arr, 0, tmp, 0, arr.length);
		return tmp;
	}
	private int[] indexesOf(String content, String key) {
		int s = 4;
		int at = 0;
		int[] arr = new int[s];
		int i = 0;
		while ((i = content.indexOf(content, i)) != -1) {
			arr[at] = i;
			at++;
			if (at == s) {
				s <<= 2;
				int[] tmp = new int[s];
				System.arraycopy(arr, 0, tmp, 0, s >>= 2);
				arr = tmp;
			}
			i += key.length();
		}
		if (at != s - 1) {
			int[] ret = new int[at + 1];
			System.arraycopy(arr, 0, ret, 0, at + 1);
			return ret;
		}
		else return arr;
	}
	private String until(String content, int index, char... keys) {
		for (int t = 0; t < content.length(); t++) {
			for (char c : keys) {
				if (content.charAt(t) == c) {
					return content.substring(index, t);
				}
			}
		}
		return null;
	}
	private boolean isNumber(char c) {
		for (char n : NUMBERS)
			if (n == c) return true;
		return false;
	}
	private boolean isKey(char c, boolean numberContext) {
		for (char k : COLOR_SPLITTERS)
			if (k == c) return true;
		return !numberContext && c == '.';
	}

	// deletes characters at the cursor
	public void delete(int amt) {
		int[] i = {0, 0};
		List<String> list = new ArrayList<>();
		section(content, list::add, () -> {}, "\n", false);
		content = list.stream()
				// sorry for this terrible, cryptic code. I don't know
				// what I was thinking when I wrote this.
				.map(in -> {
					// most of this is for catching edge cases when deleting characters,
					// keeping the cursor position valid and performing changes on the
					// text.
					if (i[0] != row - 1) {
						i[0]++;
						return in;
					} else {
						if (character - 1 > in.length())
							return in;
						if (character == 1) {
							i[0]++;
							i[1]--;
							return null;
						}
						if (in.isEmpty()) {
							i[0]++;
							i[1]--;
							return null;
						}
						if (amt >= in.length()) {
							i[0]++;
							return "";
						}
						String first = in.substring(0, character - (1 + amt));
						String after = in.substring(character - 1);
						i[0]++;
						return first + after;
					}
				})
				.filter(str -> str != null)
				.collect(Collectors.joining("\n"));
		row += i[1];
		character -= amt;
		if (character <= 0)
			character = 1;
		setText(content, top);
	}

	// inserts text at the cursor
	public void insert(String str) {
		if (content.isEmpty()) {
			content = str;
			setText(content, top);
			return;
		}
		int[] i = {0};
		List<String> list = new ArrayList<>();
		section(content, list::add, () -> {}, "\n", false);
		content = list.stream()
				.map(in -> {
					// again, catching a few edge cases
					if (i[0] != row - 1) {
						i[0]++;
						return in;
					} else {
						if (character - 1 > in.length()) {
							i[0]++;
							return in;
						}
						if (character - 1 == in.length()) {
							i[0]++;
							return in + str;
						}
						String first = in.substring(0, character - 1);
						String after = in.substring(character - 1);
						i[0]++;
						return first + str + after;
					}
				})
				.collect(Collectors.joining("\n"));
		if (!"\n".equals(str)) {
			character += str.length();
		}
		else {
			character = 1;
			row++;
		}
		setText(content, top);
	}

	public void setContent(String content) {
		setContent(content, true);
	}

	// sets the text content of this editor
	public void setContent(String content, boolean changed) {
		this.content = content;
		if (processed && changed)
			processedContent = process(content);
		setText(processed ? processedContent : content, top);
	}

	public void setProcessed(boolean processed) {
		setProcessed(processed, true);
	}
	public void setProcessed(boolean processed, boolean update) {
		boolean changed = processed != this.processed;
		this.processed = processed;
		if (changed && update) {
			if (processed)
				process();
			rebuild();
		}
	}

	// unsafe
	@Override
	public void setText(String text, int startingLine) {
		super.setText(text, startingLine);
	}

	// unsafe
	@Override
	public void setText(String text) {
		super.setText(text);
	}

	// set cursor
	@Override
	public void handleClick(int x, int y, Player player) {
		int i = 0;
		int k = -1;
		int c = 0;
		boolean over = false;
		for (Map.Entry<Integer, String> entry : stack.entries()) {
			// reset char index on new row
			if (k != entry.getKey()) {
				if (over) {
					row = entry.getKey() - 1;
					character = c + 1;
					repaint();
					return;
				}
				c = 0;
				k = entry.getKey();
			}
			String stripped = ChatColor.stripColor(entry.getValue());
			int size = stripped.length();
			// cursor is in this stack row
			if (x >= OFFSET && y >= (i * textHeight) + H_MARGIN && y < ((i + 1) * textHeight) + H_MARGIN) {
				char[] arr = stripped.toCharArray();
				int w = OFFSET;
				boolean o = false;
				for (int t = 0; t < arr.length; t++) {
					int cw = font.getChar(arr[t]).getWidth();
					// match!
					if (x >= w && x < w + cw) {
						row = entry.getKey();
						character = c + t + 1;
						repaint();
						return;
					}
					w += cw + 1;
					if (x >= w && t == arr.length - 1) {
						o = true;
					}
				}
				if (arr.length == 0)
					o = true;
				over = o;
			}
			c += size;
			i++;
		}
		if (over) {
			row = highestLine();
			character = c + 1;
			repaint();
		}
	}

	// we override painting because we need to modify it for the cursor
	// everything is prepared for us in a synchronized stack, so we don't need
	// to split lines or do anything else fancy here.
	@Override
	public void paint(CanvasGraphics g, String context) {
		g.setFont(font);
		g.drawBackground();
		int i = 0;
		int k = -1;
		int c = 0;
		int over = -1;
		// iterate through the stack
		for (Map.Entry<Integer, String> entry : stack.entries()) {
			// if the line number changed, display it
			if (k != entry.getKey()) {
				g.setFont(numberFont);
				String str = ChatColor.GRAY.toString() + entry.getKey() + ChatColor.WHITE;
				g.drawFormatted(OFFSET - (numberFont.getWidth(ChatColor.stripColor(str)) + MARGIN),
						(i * textHeight) + H_MARGIN, lastColor, str);
				g.setFont(font);
				k = entry.getKey();
				if (over >= 0) {
					for (int t = 0; t < textHeight; t++) {
						g.draw(OFFSET + over + 2, (i - 1) * textHeight + t + H_MARGIN, cursorColorSecondary);
						g.draw(OFFSET + over + 3, (i - 1) * textHeight + t + H_MARGIN, cursorColorSecondary);
					}
					over = -1;
				}
			}
			// if our cursor is on this row, we need to modify our rendering to display it
			if (entry.getKey() == row) {
				String stripped = ChatColor.stripColor(entry.getValue());
				int size = stripped.length();
				// render cursor
				if (c <= character - 1 && size + c > character - 1) {
					final int fi = c;
					final int fin = i;
					lastColor = ((ConsoleGraphics) g).drawFormatted(OFFSET,
							(i * textHeight) + H_MARGIN, lastColor, entry.getValue(),
							(index, ch, sprite, px, py) -> {
								if (index + fi == character - 1) {
									for (int t = -1; t < sprite.getWidth(); t++) {
										for (int j = (fin == 0 ? 0 : -1); j <= sprite.getHeight(); j++) {
											byte s = g.sample(px + t, py + j);
											g.draw(px + t, py + j, s == (byte) 32 ? cursorColorPrimary : cursorColorSecondary);
										}
									}
								}
							});
					i++;
					c += size;
					continue;
				}
				over = character - 1 >= size + c ? font.getWidth(stripped) : -1;
				c += size;
			}
			lastColor = g.drawFormatted(OFFSET, (i * textHeight) + H_MARGIN, lastColor, entry.getValue());
			i++;
		}
		if (over >= 0) {
			for (int t = 0; t < textHeight; t++) {
				g.draw(OFFSET + over + 2, (i - 1) * textHeight + t + H_MARGIN, cursorColorSecondary);
				g.draw(OFFSET + over + 3, (i - 1) * textHeight + t + H_MARGIN, cursorColorSecondary);
			}
		}
	}

	@Override
	public void handleInput(String input, String player) {
		if (input.startsWith("-") && input.length() >= 2) {
			String sub = input.substring(1).trim();
			try {
				int amt = Integer.parseInt(sub);
				delete(amt);
				repaint();
				return;
			}
			catch (Throwable ignored) {}
			switch (sub) {
				case "n":
					insert("\n");
					repaint();
					return;
				case "s":
					insert(" ");
					repaint();
					return;
				case "t":
					insert("    ");
					repaint();
					return;
				case "q":
					try (OutputStream out = file.createOutput()) {
						out.write(content.getBytes(Charset.forName("UTF-8")));
					} catch (IOException ignored) {}
					quit();
					return;
				case "Q":
					quit();
					return;
				case "u":
					top -= 2;
					if (top < 1)
						top = 1;
					changed();
					repaint();
					return;
				case "d":
					top += 2;
					changed();
					repaint();
					return;
				case "U":
					top = 1;
					changed();
					repaint();
					return;
				case "D":
					top = content.split("\n").length - (maxStackSize - 10) + 1;
					if (top < 1)
						top = 1 ;
					changed();
					repaint();
					return;
			}
		}
		// bad stuff happens when we use color codes, the
		// editor still works, but we need to resolve the
		// 'real' index of the cursor so it doesn't delete
		// at the wrong character index. We need to edit
		// code in this editor anyway, so there's no use
		// in supporting color.
		input = input.replace((char) 167, '&');
		insert(input);
		repaint();
	}

	public void scroll(int amt) {
		if (amt == 0) return;
		if (amt > 0) {
			top += amt;
		}
		else if (amt < 0) {
			top += amt;
			if (top < 1)
				top = 1;
		}
		rebuild();
		repaint();
	}

	public void quit() {
		computer.switchView(1);
		computer.setComponent(tty, null);
	}

	public byte getCursorColorPrimary() {
		return cursorColorPrimary;
	}

	public void setCursorColorPrimary(byte cursorColorPrimary) {
		this.cursorColorPrimary = cursorColorPrimary;
	}

	public byte getCursorColorSecondary() {
		return cursorColorSecondary;
	}

	public void setCursorColorSecondary(byte cursorColorSecondary) {
		this.cursorColorSecondary = cursorColorSecondary;
	}
}
