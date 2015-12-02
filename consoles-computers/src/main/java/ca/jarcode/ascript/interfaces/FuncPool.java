package ca.jarcode.ascript.interfaces;

import ca.jarcode.ascript.Script;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class FuncPool<T> {
	public Map<String, ScriptFunction> functions = new ConcurrentHashMap<>();

	private Thread context;

	private final T userdatum;
	private final Supplier<ScriptGlobals> globals;
	private final BooleanSupplier termaintedSupplier;

	public void mapStaticFunctions() {
		for (Map.Entry<String, Function<FuncPool, ScriptFunction>> entry : Script.STATIC_FUNCS.entrySet()) {
			functions.put(entry.getKey(), entry.getValue().apply(this));
		}
	}

	public FuncPool(Supplier<ScriptGlobals> globalsSupplier, BooleanSupplier terminatedSupplier, T userdatum) {
		this.userdatum = userdatum;
		this.globals = globalsSupplier;
		this.termaintedSupplier = terminatedSupplier;
	}

	public BooleanSupplier terminatedSupplier() {
		return termaintedSupplier;
	}

	public void register(Thread context) {
		this.context = context;
		Script.POOLS.put(context, this);
	}
	public T getUserdatum() {
		return userdatum;
	}
	public ScriptGlobals getGlobals() {
		return globals.get();
	}
	public void cleanup() {
		Script.POOLS.remove(context);
	}
}
