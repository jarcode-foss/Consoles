package jarcode.consoles.computer.interpreter;

/*

Special class when mapping functions that can be used as a way to
reference and call a function in Lua as a method parameter.

 */
@FunctionalInterface
public interface FunctionBind {
	public void call(Object... args);
}
