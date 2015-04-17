package jarcode.consoles.computer.interpreter;

@FunctionalInterface
public interface FunctionBind {
	public void call(Object... args);
}
