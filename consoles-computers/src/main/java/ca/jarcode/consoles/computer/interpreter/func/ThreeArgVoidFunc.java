package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
public interface ThreeArgVoidFunc<T1, T2, T3> {
	void call(T1 arg, T2 arg2, T3 arg3);
}
