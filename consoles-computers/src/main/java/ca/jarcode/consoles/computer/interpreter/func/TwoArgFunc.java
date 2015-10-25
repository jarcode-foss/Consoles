package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
@SuppressWarnings("unused")
public interface TwoArgFunc<R, T1, T2> {
	int C_RETURN = 1;
	int C_ARGS = 2;
	R call(T1 arg, T2 arg2);
}
