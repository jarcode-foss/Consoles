package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.manual.FunctionManual;
import jarcode.consoles.computer.manual.TypeManual;

@TypeManual(value = "Represents an interaction on the console screen. Returned from " +
		"LuaBuffer:pollCoords().",
		usage = "-- Poll coordinates from screen buffer\n" +
				"local coords = buffer:pollCoords()\n" +
				"-- Print coordinates if they exist\n" +
				"if coords ~= nil then\n" +
				"\tprint(coords:x() .. \", \" .. coords:y())\n" +
				"end")
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
