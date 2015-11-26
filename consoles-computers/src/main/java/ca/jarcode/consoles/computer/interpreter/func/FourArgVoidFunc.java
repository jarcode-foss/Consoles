package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
@SuppressWarnings("unused")
public interface FourArgVoidFunc<T1, T2, T3, T4> {
	int C_RETURN = 0;
	void call(T1 arg, T2 arg2, T3 arg3, T4 arg4);
}
