package jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
public interface TwoArgFunc<R, T1, T2> {
	public R call(T1 arg, T2 arg2);
}
