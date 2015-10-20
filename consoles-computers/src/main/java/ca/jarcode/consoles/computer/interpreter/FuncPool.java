package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.consoles.computer.Computer;
import org.luaj.vm2.lib.LibFunction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FuncPool {
	public Map<String, LibFunction> functions = new ConcurrentHashMap<>();

	private Thread context;

	private SandboxProgram program;

	public FuncPool(SandboxProgram program) {
		this.program = program;

		for (Map.Entry<String, LibFunction> entry : Lua.staticFunctions.entrySet()) {
			functions.put(entry.getKey(), entry.getValue());
		}
	}
	public void register(Thread context) {
		this.context = context;
		Lua.pools.put(context, this);
	}
	public SandboxProgram getProgram() {
		return program;
	}
	public void cleanup() {
		Lua.pools.remove(context);
	}
	public Computer getComputer() {
		return program.getComputer();
	}
}
