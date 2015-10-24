package ca.jarcode.consoles.computer.interpreter.interfaces;

import java.util.function.Consumer;

public abstract class ScriptError extends Error {
	public abstract Throwable underlying();
	public abstract String constructMessage();
}
