package ca.jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
public interface FourArgFunc<R, T1, T2, T3, T4> {
	R call(T1 arg, T2 arg2, T3 arg3, T4 arg4);
}
