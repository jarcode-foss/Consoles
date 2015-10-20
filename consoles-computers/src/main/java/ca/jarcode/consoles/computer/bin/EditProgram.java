package ca.jarcode.consoles.computer.bin;

import ca.jarcode.consoles.CColor;
import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.EditorComponent;
import ca.jarcode.consoles.computer.Terminal;
import ca.jarcode.consoles.computer.boot.Kernel;
import ca.jarcode.consoles.computer.filesystem.*;
import ca.jarcode.consoles.computer.interpreter.Lua;
import ca.jarcode.consoles.computer.interpreter.SandboxProgram;
import ca.jarcode.consoles.computer.interpreter.types.LuaFrame;
import ca.jarcode.consoles.computer.manual.ProvidedManual;
import org.bukkit.ChatColor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static ca.jarcode.consoles.computer.ProgramUtils.parseFlags;
import static ca.jarcode.consoles.computer.ProgramUtils.schedule;
import static ca.jarcode.consoles.computer.ProgramUtils.splitArguments;

@ProvidedManual(
		author = "Jarcode",
		version = "2.14",
		contents = "Opens up an editor for the given file. Navigation commands are prefixed " +
				"by the dash '-' character. The editor is opened up in a separate screen " +
				"screen session. Commands in the editor are as follows:\n\n" +
				"\u00A7e/-n\u00A7f adds a new line\n" +
				"\u00A7e/-q\u00A7f saves and quits\n" +
				"\u00A7e/-Q\u00A7f quits without saving\n" +
				"\u00A7e/-u\u00A7f scrolls up by two lines\n" +
				"\u00A7e/-d\u00A7f scrolls down by two lines\n" +
				"\u00A7e/-U\u00A7f scrolls to the top of the file\n" +
				"\u00A7e/-D\u00A7f scrolls to the bottom of the file\n" +
				"\u00A7e/-t\u00A7f adds four spaces (tab)"
)
@SuppressWarnings("unused")
public class EditProgram extends FSProvidedProgram {
	private static final String DEFAULT_CONFIG = "-- This is preloaded by the edit program to " +
			"set configuration values. You may edit this to your preference.\n\n" +
			"-- Color mappings set for certain symbols\n" +
			"symbolColor(\"()\", \"9\")\n" +
			"symbolColor(\"{}\", \"d\")\n" +
			"-- Color used for code comments\n" +
			"commentColor(\"2\")\n" +
			"-- String color\n" +
			"stringColor(\"a\")\n" +
			"-- Lua keyword color\n" +
			"keywordColor(\"c\")\n" +
			"-- Raw map color of line numbers on the left side of the editor\n" +
			"lineNumberColor(36)";

	Charset charset = Charset.forName("UTF-8");

	@Override
	public void run(String str, Computer computer) throws Exception {

		String[] args = splitArguments(str);
		HashMap<String, Object> properties = new HashMap<>();
		properties.put("stripped", false);
		properties.put("line", -1);
		if (args.length == 0 || str.isEmpty()) {
			println("Usage: edit [FILE] [OPTION]...");
			println("Flags:");
			println("\t-s\tstrips the editor of all color formatting");
			println("\t-n\tsets which line to set the editor view to");
			return;
		}

		args = parseFlags(args, (flag, string) -> {
			switch (flag) {
				case 's':
					properties.put("stripped", true);
					break;
				case 'n':
					try {
						int line = Integer.parseInt(string);
						if (line < 1)
							print("edit: invalid line: " + string);
						else properties.put("line", line);
					}
					catch (NumberFormatException e) {
						print("edit: invalid line: " + string);
					}
					break;
			}
		}, c -> c == 'n');
		if (args.length == 0) {
			print("edit: no file given");
			return;
		}
		str = args[0];
		int index = (Integer) properties.get("line");
		Terminal terminal = computer.getTerminal(this);
		FSBlock block = computer.getBlock(str, terminal.getCurrentDirectory());
		if (block == null) {
			print("edit: " + str.trim() + ": no such file");
			return;
		}
		if (!(block instanceof FSFile)) {
			print("edit: " + str.trim() + ": not a file");
			return;
		}
		FSFile file = (FSFile) block;
		str = readFully(file);
		final String finalStr = str.replace("\r", "");

		FSBlock config = computer.getBlock("/etc/edit/config", terminal.getCurrentDirectory());
		if (config == null) {
			print("/etc/edit/config does not exist, creating...\n");
			if (computer.getBlock("/etc/edit", terminal.getCurrentDirectory()) == null)
				computer.getRoot().mkdir("etc/edit");
			FSFolder parent = (FSFolder) computer.getBlock("/etc/edit", terminal.getCurrentDirectory());
			config = Kernel.writtenFile(DEFAULT_CONFIG, computer);
			parent.contents.put("config", config);
		}
		if (!(config instanceof FSFile)) {
			print("edit: /etc/edit/config: not a file\n");
			return;
		}

		new EditorInstance().scheduleCreate(computer, file, (FSFile) config, finalStr,
				index, (Boolean) properties.get("stripped"));
	}

	private char translateColorChar(String color) {
		if (color.length() == 0) return 0;
		char c = color.toLowerCase().charAt(0);
		if (!CColor.colorCharRange(c)) return 0;
		return c;
	}

	private String readFully(FSFile file) throws IOException, InterruptedException {
		boolean printed = false;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (InputStream is = file.createInput()) {
			int i;
			while (true) {
				if (terminated())
					break;
				if (is.available() > 0 || is instanceof ByteArrayInputStream) {
					i = is.read();
					if (i == -1) break;
					out.write((byte) i);
				} else {
					if (!printed) {
						print("reading file, waiting for EOF or termination...");
						printed = true;
					}
					Thread.sleep(50);
				}
			}
			if (terminated())
				print("\tterminated");
		}
		return new String(out.toByteArray(), charset);
	}

	private class EditorInstance {
		String commentColor = ChatColor.DARK_GREEN.toString(),
				stringColor = ChatColor.GREEN.toString(),
				keywordColor = ChatColor.RED.toString();
		List<char[]> keyCharList = new ArrayList<>();
		List<String> keyColorList = new ArrayList<>();
		byte numberColor = 36;

		void scheduleCreate(Computer computer, FSFile file, FSFile config, String content, int index, boolean stripped)
				throws IOException, InterruptedException {

			SandboxProgram inst = SandboxProgram.FACTORY.get();
			Lua.find(EditorInstance.class, this, inst.getPool());
			SandboxProgram.pass(inst, readFully(config), computer.getTerminal(EditProgram.this), instance, "");

			schedule(() -> {
				EditorComponent component = new EditorComponent(computer.getViewWidth(),
						computer.getViewHeight(), computer, file, 7);

				if (!stripped) {
					component.registerProcessor(EditorComponent::findCommentRanges, commentColor);
					component.registerProcessor(EditorComponent::findStringRanges, stringColor);
					component.registerProcessor(EditorComponent::findKeywordRanges, keywordColor);

					for (int t = 0; t < keyCharList.size(); t++) {
						final int ft = t;
						component.registerProcessor(
								(s) -> EditorComponent.findBracketRanges(s, keyCharList.get(ft)),
								keyColorList.get(t));
					}
				}
				else component.setProcessed(false, false);

				component.setNumberColor(numberColor);

				if (index != -1)
					component.setView(index);
				component.setContent(content);
				computer.setComponent(7, component);
				computer.switchView(8);
			});
		}

		public void lua$symbolColor(String symbols, String color) {
			char c = translateColorChar(color);
			if (c == 0) return;
			keyCharList.add(symbols.toCharArray());
			keyColorList.add(ChatColor.COLOR_CHAR + "" + c);
		}

		public void lua$commentColor(String color) {
			char c = translateColorChar(color);
			if (c == 0) return;
			commentColor = ChatColor.COLOR_CHAR + "" + c;
		}

		public void lua$stringColor(String color) {
			char c = translateColorChar(color);
			if (c == 0) return;
			stringColor = ChatColor.COLOR_CHAR + "" + c;
		}

		public void lua$keywordColor(String color) {
			char c = translateColorChar(color);
			if (c == 0) return;
			keywordColor = ChatColor.COLOR_CHAR + "" + c;
		}

		public void lua$lineNumberColor(int color) {
			numberColor = LuaFrame.convert(color);
		}
	}
}
