package jarcode.consoles.computer.interpreter;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.function.Supplier;

public class ComputerLibrary {
	private String name;
	public final boolean isRestricted;
	private Supplier<TwoArgFunction> supplier;
	public ComputerLibrary(String name, boolean isRestricted, Supplier<NamedFunction[]> functions) {
		this.name = name;
		this.isRestricted = isRestricted;
		supplier = () -> new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored, LuaValue global) {
				LuaTable table = new LuaTable(0, 30);
				global.set(name, table);
				for (NamedFunction function : functions.get()) {
					table.set(function.mappedName, function);
				}
				global.get("package").get("loaded").set(name, table);
				return table;
			}
		};
	}
	public TwoArgFunction buildLibrary() {
		return supplier.get();
	}
	public static class NamedFunction extends LibFunction {
		String mappedName;
		public void setName(String name) {
			this.mappedName = name;
		}
	}
}
