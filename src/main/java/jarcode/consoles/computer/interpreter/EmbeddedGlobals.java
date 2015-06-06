package jarcode.consoles.computer.interpreter;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.List;

public class EmbeddedGlobals extends Globals {

	private List<String> finalized = new ArrayList<>();

	public void finalizeEntries() {
		for (LuaValue key : keys()) {
			if (key.isstring())
				finalized.add(key.checkjstring());
		}
	}

	@Override
	public void set(String str, LuaValue value) {
		if (finalized.contains(str)) error("cannot change final variable");
		else super.set(str, value);
	}
	@Override
	public void set(LuaValue arg, LuaValue value) {
		if (arg.isstring() && finalized.contains(arg.checkjstring()))
			error("cannot change final variable");
		if (arg.isint())
			error("cannot index globals with integers");
	}
	@Override
	public void set(int i, LuaValue value) {
		error("cannot index globals with integers");
	}
	@Override
	public void set(String str, String value) {
		if (finalized.contains(str))
			error("cannot change final variable");
		else super.set(str, value);
	}
	@Override
	public void set(int i, String value) {
		error("cannot index globals with integers");
	}
	@Override
	public void set(String str, int value) {
		if (finalized.contains(str))
			error("cannot change final variable");
		else super.set(str, value);
	}
}
