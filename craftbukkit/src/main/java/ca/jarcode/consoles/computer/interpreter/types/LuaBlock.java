package ca.jarcode.consoles.computer.interpreter.types;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.filesystem.FSBlock;
import ca.jarcode.consoles.computer.filesystem.FSFolder;
import ca.jarcode.consoles.computer.manual.FunctionManual;
import ca.jarcode.consoles.computer.manual.TypeManual;

import java.util.Arrays;
import java.util.stream.Collectors;

@TypeManual("A block in the filesystem, which can represent any kind of file or folder. " +
		"This type is abstract and can not be instantiated.")
@SuppressWarnings("unused")
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

	@FunctionManual("Deletes this block from the filesystem.")
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

	@FunctionManual("Returns the path of this block in the filesystem.")
	public String getPath() {
		return path;
	}

	@FunctionManual("Returns the name of the block in the filesystem.")
	public String getName() {
		String[] arr = FSBlock.section(path, "/");
		return Arrays.asList(arr).stream()
				.filter(s -> !s.isEmpty())
				.reduce((o1, o2) -> o2)
				.get();
	}

	@FunctionManual("Returns the type of this filesystem block as a string.")
	public String getBlockType() {
		return this.getClass().getSimpleName();
	}
}
