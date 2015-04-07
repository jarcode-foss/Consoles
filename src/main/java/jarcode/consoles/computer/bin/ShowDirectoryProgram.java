package jarcode.consoles.computer.bin;

import org.bukkit.ChatColor;

import java.util.Map;

public class ShowDirectoryProgram extends FSProvidedProgram {
	@Override
	public void run(String str, Computer computer) throws Exception {
		Terminal terminal = computer.getTerminal(this);
		String cd = terminal.getCurrentDirectory();
		FSBlock block = computer.getRoot().get(cd);
		if (!(block instanceof FSFolder)) {
			println("Invalid current directory");
			return;
		}
		FSFolder folder = (FSFolder) block;
		println(cd);
		nextln();
		StringBuilder builder = new StringBuilder();
		int index = 0;
		for (Map.Entry<String, FSBlock> entry : folder.contents.entrySet()) {
			if (entry.getValue() instanceof FSFolder)
				builder.append(ChatColor.BLUE);
			else if (entry.getValue() instanceof FSProvidedProgram)
				builder.append(ChatColor.RED);
			else
				builder.append(ChatColor.WHITE);
			builder.append(entry.getKey());
			if (index != folder.contents.size() - 1)
				builder.append("\t");
		}
		println(builder.toString());
	}
}
