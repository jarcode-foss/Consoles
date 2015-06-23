package jarcode.consoles.computer.interpreter.func;

@FunctionalInterface
public interface OneArgVoidFunc<T1> {
	public void call(T1 arg);
}
