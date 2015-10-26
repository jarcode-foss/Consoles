package ca.jarcode.consoles.computer.interpreter.luanative;

// implementation type for native lua
public enum LuaNImpl {

	DEFAULT(0), JIT(1);

	public final int val;

	LuaNImpl(int val) {
		this.val = val;
	}
}
