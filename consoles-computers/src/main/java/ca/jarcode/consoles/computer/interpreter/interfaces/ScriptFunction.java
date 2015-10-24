package ca.jarcode.consoles.computer.interpreter.interfaces;

public interface ScriptFunction {
	default ScriptValue call() {
		return call(new ScriptValue[0]);
	}
	ScriptValue call(ScriptValue... args);
	ScriptValue getAsValue();
}
