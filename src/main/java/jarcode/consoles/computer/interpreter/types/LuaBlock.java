package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFolder;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class LuaBlock {

	protected final String path, cd;
	protected final FSBlock block;
	protected final Computer computer;

	public LuaBlock(FSBlock block, String path, String cd, Computer computer) {
		this.path = path;
		this.cd = cd;
		this.block = block;
		this.computer = computer;
	}

	public boolean delete() {
		String[] arr = FSBlock.section(path, "/");
		String f = Arrays.asList(arr).stream()
				.limit(arr.length == 0 ? 0 : arr.length - 1)
				.collect(Collectors.joining("/"));
		String n = Arrays.asList(arr).stream()
				.filter(s -> !s.isEmpty())
				.reduce((o1, o2) -> o2)
				.get();
		FSBlock b = computer.getBlock(f, cd);
		if (b instanceof FSFolder) {
			if (!((FSFolder) b).contents.containsKey(n))
				return false;
			((FSFolder) b).contents.remove(n);
			return true;
		}
		else return false;
	}
	public String getPath() {
		return path;
	}
}
