package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptFunction;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptGlobals;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class FuncPool {
	public Map<String, ScriptFunction> functions = new ConcurrentHashMap<>();

	private Thread context;

	private final SandboxProgram program;
	private final Supplier<ScriptGlobals> globals;

	public void mapStaticFunctions() {
		for (Map.Entry<String, Function<FuncPool, ScriptFunction>> entry : Lua.staticFunctions.entrySet()) {
			functions.put(entry.getKey(), entry.getValue().apply(this));
		}
	}

	public FuncPool(SandboxProgram program) {
		this.program = program;
		this.globals = () -> program.globals;
	}

	public FuncPool(Supplier<ScriptGlobals> globalsSupplier) {
		program = null;
		this.globals = globalsSupplier;
	}

	public void register(Thread context) {
		this.context = context;
		Lua.pools.put(context, this);
	}
	public SandboxProgram getProgram() {
		if (program == null)
			throw new IllegalStateException("program is null");
		return program;
	}
	public ScriptGlobals getGlobals() {
		return globals.get();
	}
	public void cleanup() {
		Lua.pools.remove(context);
	}
	public Computer getComputer() {
		if (program == null)
			throw new IllegalStateException("program is null");
		return program.getComputer();
	}
}
