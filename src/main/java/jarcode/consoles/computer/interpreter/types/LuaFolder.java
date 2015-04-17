package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSFolder;

import java.util.function.BooleanSupplier;

public class LuaFolder extends LuaBlock {
	private final FSFolder file;
	private BooleanSupplier supplier;
	public LuaFolder(FSFolder file, String path, String cd, BooleanSupplier supplier, Computer computer) {
		super(file, path.endsWith("/") ? path.substring(0, path.length() - 1) : path, cd, computer);
		this.file = file;
		this.supplier = supplier;
	}
	public String[] list() {
		return file.contents.keySet().stream().toArray(String[]::new);
	}
	public LuaFile[] files() {
		return file.contents.entrySet().stream()
				.filter((entry) -> entry.getValue() instanceof FSFile)
				.map((entry) -> new LuaFile((FSFile) entry.getValue(), path + "/" + entry.getKey(),
						cd, supplier, computer))
				.toArray(LuaFile[]::new);
	}
}
