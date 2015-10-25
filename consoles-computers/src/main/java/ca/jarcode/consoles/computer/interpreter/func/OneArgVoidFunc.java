package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
@SuppressWarnings("unused")
public interface OneArgVoidFunc<T1> {
	int C_RETURN = 0;
	int C_ARGS = 1;
	void call(T1 arg);
}
