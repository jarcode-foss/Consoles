package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
@SuppressWarnings("unused")
public interface NoArgVoidFunc {
	int C_RETURN = 0;
	int C_ARGS = 0;
	void call();
}
