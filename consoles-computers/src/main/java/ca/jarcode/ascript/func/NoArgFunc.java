package ca.jarcode.ascript.func;

@FunctionalInterface
@SuppressWarnings("unused")
public interface NoArgFunc <R> {
	int C_RETURN = 1;
	R call();
}
