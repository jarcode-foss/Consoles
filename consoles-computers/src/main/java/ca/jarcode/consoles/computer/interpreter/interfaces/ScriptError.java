package ca.jarcode.consoles.computer.interpreter.interfaces;

public abstract class ScriptError extends Error {
	public abstract Throwable underlying();
	public abstract String constructMessage();
}
