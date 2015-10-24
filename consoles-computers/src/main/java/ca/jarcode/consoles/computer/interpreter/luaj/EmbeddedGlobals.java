package ca.jarcode.consoles.computer.interpreter.luaj;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static ca.jarcode.consoles.Lang.lang;

public class EmbeddedGlobals extends Globals {

	InterruptLib interruptLib;
	boolean restricted = true;

	private List<LuaValue> finalized = new ArrayList<>();

	EmbeddedGlobals(BooleanSupplier terminated) {
		interruptLib = new InterruptLib(terminated);
	}

	public void finalizeEntries() {
		for (LuaValue key : keys()) {
			if (key.isstring())
				finalized.add(get(key));
		}
	}

	@Override
	public void set(String str, LuaValue value) {
		if (str == null)
			error("cannot index null");
		else if (finalized.contains(get(str)))
			finalErr();
		else super.set(str, value);
	}
	@Override
	public void set(LuaValue arg, LuaValue value) {
		if (finalized.contains(get(arg)))
			finalErr();
		super.set(arg, value);
	}
	@Override
	public void set(int i, LuaValue value) {
		if (finalized.contains(get(i)))
			finalErr();
		super.set(i, value);
	}

	@Override
	public void set(String str, String value) {
		if (finalized.contains(get(str)))
			finalErr();
		else super.set(str, value);
	}
	@Override
	public void set(int i, String value) {
		if (finalized.contains(get(i)))
			finalErr();
		super.set(i, value);
	}
	@Override
	public void set(String str, int value) {
		if (finalized.contains(get(str)))
			finalErr();
		else super.set(str, value);
	}
	private static void finalErr() {
		error(lang.getString("lua-final-var"));
	}
}
