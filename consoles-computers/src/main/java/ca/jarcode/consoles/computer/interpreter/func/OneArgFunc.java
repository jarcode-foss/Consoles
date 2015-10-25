package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
@SuppressWarnings("unused")
public interface OneArgFunc<R, T> {
	int C_RETURN = 1;
	int C_ARGS = 1;
	R call(T arg);
}
