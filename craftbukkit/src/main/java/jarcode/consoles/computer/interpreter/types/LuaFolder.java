package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.manual.FunctionManual;
import jarcode.consoles.computer.manual.TypeManual;

import java.util.function.BooleanSupplier;

@TypeManual("A folder that exists in the filesystem.")
@SuppressWarnings("unused")
public class LuaFolder extends LuaBlock {

	private final FSFolder file;
	private BooleanSupplier supplier;

	public LuaFolder(FSFolder file, String path, String cd, BooleanSupplier supplier, Computer computer) {
		super(file, path.endsWith("/") ? path.substring(0, path.length() - 1) : path, cd, computer);
		this.file = file;
		this.supplier = supplier;
	}
	@FunctionManual("Returns a list of all the entries in this folder as strings")
	public String[] list() {
		return file.contents.keySet().stream().toArray(String[]::new);
	}
	@FunctionManual("Returns all stored files and folders inside of this folder. Ignores provided " +
			"programs.")
	public LuaBlock[] blocks() {
		return file.contents.entrySet().stream()
				.filter((entry) -> entry.getValue() instanceof FSFile || entry.getValue() instanceof FSFolder)
				.map((entry) -> entry.getValue() instanceof FSFile ?
						new LuaFile((FSFile) entry.getValue(), path + "/" + entry.getKey(),
						cd, supplier, computer) : new LuaFolder((FSFolder) entry.getValue(), path + "/" + entry.getKey(),
						cd, supplier, computer))
				.toArray(LuaBlock[]::new);

	}
	@FunctionManual("Returns a list of all the files inside of this folder (ignores folders)")
	public LuaFile[] files() {
		return file.contents.entrySet().stream()
				.filter((entry) -> entry.getValue() instanceof FSFile)
				.map((entry) -> new LuaFile((FSFile) entry.getValue(), path + "/" + entry.getKey(),
						cd, supplier, computer))
				.toArray(LuaFile[]::new);
	}
}
