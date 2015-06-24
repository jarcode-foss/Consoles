package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
public interface TwoArgVoidFunc<T1, T2> {
	public void call(T1 arg, T2 arg2);
}
