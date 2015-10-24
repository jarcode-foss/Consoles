package ca.jarcode.consoles.computer.interpreter.luaj;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.jse.JseMathLib;

public class LuaJEmbeddedMathLib extends JseMathLib {

	@Override
	public LuaValue call(LuaValue str, LuaValue globals) {
		LuaValue ret = super.call(str, globals);
		LuaValue var3 = globals.get("math");
		var3.set("round", new Round());
		return ret;
	}
	public static class Round extends OneArgFunction {

		@Override
		public LuaValue call(LuaValue luaValue) {
			if (luaValue.isnumber()) {
				return valueOf((int) Math.round(luaValue.checkdouble()));
			}
			else throw new IllegalArgumentException("bad argument");
		}
	}
}
