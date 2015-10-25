package ca.jarcode.consoles.computer.interpreter.func;

@SuppressWarnings("unused")
@FunctionalInterface
public interface TwoArgVoidFunc<T1, T2> {
	int C_RETURN = 0;
	int C_ARGS = 2;
	void call(T1 arg, T2 arg2);
}
