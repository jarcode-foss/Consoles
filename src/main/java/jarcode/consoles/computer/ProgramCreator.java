package jarcode.consoles.computer;

import jarcode.consoles.ConsoleFeed;
import jarcode.consoles.computer.filesystem.*;
import jarcode.consoles.computer.interpreter.InterpretedProgram;
import net.md_5.bungee.api.ChatColor;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProgramCreator implements ConsoleFeed.FeedCreator {

	private String result = null;
	private ProgramInstance lastInstance;
	private final Terminal terminal;

	public ProgramCreator(Terminal terminal) {
		this.terminal = terminal;
	}

	@Override
	public void from(String input) {

		String argument;

		// string encapsulation
		if (input.startsWith("\"")) {

			StringBuilder builder = new StringBuilder();

			char[] arr = input.toCharArray();
			for (int t = 1; t < builder.length(); t++) {
				if (arr[t] != '\"' && (t == 1 || arr[t - 1] != '\\')) {
					builder.append(arr[t]);
				}
				else break;
			}
			if (input.length() > builder.length())
				argument = input.substring(builder.length() + 1);
			else
				argument = "";
			input = builder.toString();
		}
		else {
			String[] split = input.split(" ");
			if (split.length > 1) {
				argument = input.substring(split[0].length() + 1);
			}
			else argument = "";
			input = split[0];
		}

		FSBlock block;
		String old = input;
		// start from root
		if (input.startsWith("/")) {
			input = input.substring(1);
			try {
				block = terminal.getComputer().getRoot().get(input);
			}
			catch (FileNotFoundException e) {
				result = old + ": program not found";
				return;
			}
		}
		// system path or current dir
		else check: if (!input.contains("/")) {

			// cd
			block = terminal.getComputer().getBlock(input, terminal.getCurrentDirectory());
			if (block != null)
				break check;
			// loop through path entries if no match in current directory
			for (String path : terminal.getComputer().getSystemPath()) {
				try {
					FSBlock pathBlock = terminal.getComputer().getRoot().get(path);
					if (pathBlock instanceof FSFolder) {
						FSFolder folder = (FSFolder) pathBlock;
						try {
							block = folder.get(input);
							break check;
						}
						// program doesn't exist in this path entry, try another
						catch (FileNotFoundException ignored) {}
					}
					else {
						result = old + ": system path entry does not point to a file (" + path + ")";
						return;
					}
				}
				// invalid path variable
				catch (FileNotFoundException ignored) {
					result = old + ": invalid system path";
					return;
				}
			}
			result = old + ": program not found";
			return;
		}
		// start from current directory
		else {
			try {
				block = terminal.getComputer().getRoot().get(terminal.getCurrentDirectory());
				try {
					block = ((FSFolder) block).get(input);
					if (!(block instanceof FSFile || block instanceof FSProvidedProgram)) {
						result = "invalid path: must be a file or provided program";
						return;
					}
				} catch (FileNotFoundException e) {
					result = old + ": program not found";
					return;
				}
			}
			catch (FileNotFoundException e) {
				result = ChatColor.RED + "invalid current directory. Restart your terminal instance!";
				return;
			}
		}
		// try running the program
		result = tryBlock(block, argument, terminal.getUser());
	}

	@Override
	public String result() {
		return result;
	}

	@Override
	public InputStream getInputStream() {
		return lastInstance.in;
	}

	@Override
	public OutputStream getOutputStream() {
		return lastInstance.out;
	}

	@Override
	public ConsoleFeed.FeedEncoder getEncoder() {
		return ConsoleFeed.UTF_ENCODER;
	}
	public ProgramInstance getLastInstance() {
		return lastInstance;
	}
	String tryBlock(FSBlock target, String argument, String user) {
		ProgramInstance instance;
		if ((target instanceof FSFile || target instanceof FSProvidedProgram)) {
			if (target.getOwner().equals(user))
				if (!target.check(FSGroup.OWNER, 'x'))
					return "permission denied";
			else
				if (!target.check(FSGroup.ALL, 'x'))
					return "permission denied";
		}
		if (target instanceof FSFile) {
			instance = new ProgramInstance(new InterpretedProgram((FSFile) target), argument, terminal.getComputer());
		}
		else if (target instanceof FSProvidedProgram) {
			instance = new ProgramInstance((FSProvidedProgram) target, argument, terminal.getComputer());
		}
		else {
			return "invalid path: must be a file or provided program";
		}
		try {
			instance.start();
		}
		catch (Throwable e) {
			return ChatColor.RED + "unable to start thread: " + e.getClass().getSimpleName();
		}
		result = null;
		lastInstance = instance;
		return null;
	}
}
