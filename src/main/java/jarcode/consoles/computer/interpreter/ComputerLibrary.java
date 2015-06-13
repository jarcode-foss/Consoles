package jarcode.consoles.computer.interpreter;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.function.Supplier;

public class ComputerLibrary {
	public final boolean isRestricted;
	private final Supplier<TwoArgFunction> supplier;
	public ComputerLibrary(String libraryName, boolean isRestricted, Supplier<NamedFunction[]> functions) {
		this.isRestricted = isRestricted;
		if (libraryName == null) {
			throw new NullPointerException("name cannot be null");
		}
		supplier = () -> new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue ignored, LuaValue global) {
				LuaTable table = new LuaTable(0, 30);
				global.set(libraryName, table);
				for (NamedFunction function : functions.get()) {
					table.set(function.mappedName, function);
				}
				global.get("package").get("loaded").set(libraryName, table);
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
