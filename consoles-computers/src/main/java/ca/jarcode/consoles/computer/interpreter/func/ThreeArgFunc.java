package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
public interface ThreeArgFunc<R, T1, T2, T3> {
	R call(T1 arg, T2 arg2, T3 arg3);
}
