package jarcode.consoles.computer.interpreter;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.Map;

public class GameLib extends TwoArgFunction {

	@Override
	public LuaValue call(LuaValue ignored, LuaValue env) {
		LuaTable table = new LuaTable();
		for (Map.Entry<String, LibFunction> entry : Lua.staticFunctions.entrySet()) {
			table.set(entry.getKey(), entry.getValue());
		}
		env.set("game", table);
		env.get("package").get("loaded").set("game", table);
		return table;
	}
}
