package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
public interface OneArgFunc<R, T> {
	public R call(T arg);
}
