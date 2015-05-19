package jarcode.consoles.computer.interpreter.types;

@SuppressWarnings("unused")
public class LuaInteraction {
	private final int x, y;
	private final String context;
	public LuaInteraction(int x, int y, String context) {
		this.x = x;
		this.y = y;
		this.context = context;
	}
	public int x() {
		return x;
	}
	public int y() {
		return y;
	}
	public String name() {
		return context;
	}
}
