package ca.jarcode.ascript.interfaces;

public abstract class ScriptError extends Error {
	public abstract Throwable underlying();
	public abstract String constructMessage();
}
