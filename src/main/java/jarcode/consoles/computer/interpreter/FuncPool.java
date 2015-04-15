package jarcode.consoles.computer.interpreter;

import jarcode.consoles.computer.Computer;
import org.luaj.vm2.lib.LibFunction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FuncPool {
	public Map<String, LibFunction> functions = new ConcurrentHashMap<>();
	private Computer computer;
	private Thread context;
	public FuncPool(Thread context, Computer computer) {
		this.computer = computer;
		this.context = context;
		Lua.pools.put(context, this);

		for (Map.Entry<String, LibFunction> entry : Lua.staticFunctions.entrySet()) {
			functions.put(entry.getKey(), entry.getValue());
		}
	}
	public void cleanup() {
		Lua.pools.remove(context);
	}
	public Computer getComputer() {
		return computer;
	}
}
