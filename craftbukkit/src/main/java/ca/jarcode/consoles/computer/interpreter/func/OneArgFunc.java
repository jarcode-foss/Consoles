package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
public interface OneArgFunc<R, T> {
	R call(T arg);
}
