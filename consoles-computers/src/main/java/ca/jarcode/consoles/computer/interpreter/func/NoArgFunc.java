package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
@SuppressWarnings("unused")
public interface NoArgFunc <R> {
	int C_RETURN = 1;
	int C_ARGS = 0;
	R call();
}
