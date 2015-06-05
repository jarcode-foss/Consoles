package jarcode.consoles.computer.interpreter;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class ComputerLibrary extends TwoArgFunction {
	private String name;
	private NamedFunction[] functions;
	public final boolean isRestricted;
	public ComputerLibrary(String name, NamedFunction[] functions, boolean isRestricted) {
		this.name = name;
		this.functions = functions;
		this.isRestricted = isRestricted;
	}
	@Override
	public LuaValue call(LuaValue ignored, LuaValue global) {
		LuaTable table = new LuaTable(0, 30);
		global.set(name, table);
		for (NamedFunction function : functions) {
			table.set(function.mappedName, function);
		}
		global.get("package").get("loaded").set(name, table);
		return table;
	}
	public static class NamedFunction extends LibFunction {
		String mappedName;
		public void setName(String name) {
			this.mappedName = name;
		}
	}
}
