package ca.jarcode.ascript.luanative;

// implementation type for native lua
public enum LuaNImpl {

	DEFAULT(0), JIT(1), JIT_TEST(1);

	public final int val;

	LuaNImpl(int val) {
		this.val = val;
	}
}
