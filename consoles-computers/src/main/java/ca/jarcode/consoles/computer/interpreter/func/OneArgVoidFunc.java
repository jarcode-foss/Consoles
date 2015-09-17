package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
public interface OneArgVoidFunc<T1> {
	void call(T1 arg);
}
