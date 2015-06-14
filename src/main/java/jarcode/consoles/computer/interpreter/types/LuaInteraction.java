package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.manual.FunctionManual;

@SuppressWarnings("unused")
public class LuaInteraction {
	private final int x, y;
	private final String context;
	public LuaInteraction(int x, int y, String context) {
		this.x = x;
		this.y = y;
		this.context = context;
	}
	@FunctionManual("Returns the X coordinate of this interaction.")
	public int x() {
		return x;
	}
	@FunctionManual("Returns the Y coordinate of this interaction.")
	public int y() {
		return y;
	}
	@FunctionManual("Returns the name of the player who interacted with the console.")
	public String name() {
		return context;
	}
}
