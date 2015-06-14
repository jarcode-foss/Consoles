package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.Consoles;
import jarcode.consoles.computer.manual.ManualManager;

public class LuaTypes {
	public static void init() {
		reg(LuaArray.class);
		reg(LuaBlock.class);
		reg(LuaBuffer.class);
		reg(LuaChest.class);
		reg(LuaComputer.class);
		reg(LuaFile.class);
		reg(LuaFolder.class);
		reg(LuaFrame.class);
		reg(LuaInteraction.class);
		if (Consoles.componentRenderingEnabled)
			reg(LuaPainter.class);
		reg(LuaTerminal.class);
		reg(LuaTypeBuilder.class);
	}
	private static void reg(Class<?> type) {
		ManualManager.loadType(type);
	}
}
