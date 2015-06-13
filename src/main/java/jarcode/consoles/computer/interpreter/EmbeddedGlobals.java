package jarcode.consoles.computer.interpreter;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EmbeddedGlobals extends Globals {

	private List<LuaValue> finalized = new ArrayList<>();

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
			error("cannot change final variable");
		else super.set(str, value);
	}
	@Override
	public void set(LuaValue arg, LuaValue value) {
		if (finalized.contains(get(arg)))
			error("cannot change final variable");
		super.set(arg, value);
	}
	@Override
	public void set(int i, LuaValue value) {
		if (finalized.contains(get(i)))
			error("cannot change final variable");
		super.set(i, value);
	}
	@Override
	public void set(String str, String value) {
		if (finalized.contains(get(str)))
			error("cannot change final variable");
		else super.set(str, value);
	}
	@Override
	public void set(int i, String value) {
		if (finalized.contains(get(i)))
			error("cannot change final variable");
		super.set(i, value);
	}
	@Override
	public void set(String str, int value) {
		if (finalized.contains(get(str)))
			error("cannot change final variable");
		else super.set(str, value);
	}
}
