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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
	private static final String[] OPERATORS = { "+", "-", "*", "/", "%", "^",
			"#", "==", "~=", "<=", ">=", "<", ">", "=", "(", ")", "{", "}",
			"[", "]", ";", ":", ",", ".", "..", "..."};

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
	public void rebuild() {
		setContent(content);
	}
	public String color(String content) {
		StringBuilder builder = new StringBuilder();
		return null;
		//TODO: finish
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

	// sets the text content of this editor
	public void setContent(String content) {
		this.content = content;
		setText(content, top);
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
					rebuild();
					repaint();
					return;
				case "d":
					top += 2;
					rebuild();
					repaint();
					return;
				case "U":
					top = 1;
					rebuild();
					repaint();
					return;
				case "D":
					top = content.split("\n").length - (maxStackSize - 10) + 1;
					if (top < 1)
						top = 1 ;
					rebuild();
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
