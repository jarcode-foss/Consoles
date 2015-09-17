package ca.jarcode.consoles.computer.manual;

import org.bukkit.ChatColor;

import java.util.function.Consumer;
import java.util.function.Function;

public class ManualEntry {

	private final String author, version, contents, usage, args;

	private final Function<String, String> header;

	public ManualEntry(Function<String, String> header, String author, String contents,
	                   String version, String usage, String args) {
		this.author = author;
		this.contents = contents;
		this.version = version;
		this.usage = usage;
		this.header = header;
		this.args = args;
	}

	public String getHeader(String programName) {
		return header.apply(programName);
	}

	public String getAuthor() {
		return author;
	}

	public String getContents() {
		return contents;
	}

	public String getVersion() {
		return version;
	}

	public String getUsage() {
		return usage;
	}

	public String getArgs() {
		return args;
	}
	public String getText(String programName) {
		StringBuilder builder = new StringBuilder();
		Consumer<String> title = (t) -> {
			builder.append(ChatColor.GREEN);
			builder.append(t);
			builder.append(ChatColor.WHITE);
		};
		for (int t = 0; t < 8; t++)
			builder.append('\t');
		builder.append(getHeader(programName));
		builder.append('\n');
		builder.append('\n');
		if (getAuthor() != null) {
			title.accept("AUTHOR: ");
			builder.append(getAuthor());
			if (getContents() != null || getUsage() != null || getVersion() != null)
				builder.append("\n\n");
		}
		if (getVersion() != null) {
			title.accept("VERSION: ");
			builder.append(getVersion());
			if (getContents() != null || getUsage() != null)
				builder.append("\n\n");
		}
		if (getContents() != null) {
			title.accept("DESCRIPTION: ");
			builder.append(getContents());
			if (getUsage() != null)
				builder.append("\n\n");
		}
		if (getUsage() != null) {
			title.accept("USAGE: ");
			builder.append(getUsage());
			if (getArgs() != null) {
				builder.append("\n\n");
				builder.append(getArgs());
			}
		}
		return builder.toString();
	}
}
