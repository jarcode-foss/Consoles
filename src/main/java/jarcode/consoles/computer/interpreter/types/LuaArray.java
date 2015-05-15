package jarcode.consoles.computer.interpreter.types;

import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

public class LuaArray extends LuaValue {

	LuaValue[] arr;

	public LuaArray(int size) {
		arr = new LuaValue[size];
	}

	@Override
	public int type() {
		return 85;
	}

	@Override
	public String typename() {
		return "static_array";
	}

	@Override
	public LuaValue get(LuaValue value) {
		if (value.isint())
			return resolve(value.checkint());
		else if (value.isstring() && value.checkjstring().equals("len"))
			return LuaInteger.valueOf(arr.length);
		else return error("not integer");
	}
	@Override
	public LuaValue get(int i) {
		return resolve(i);
	}
	@Override
	public LuaValue get(String str) {
		if (str.equals("len"))
			return LuaInteger.valueOf(arr.length);
		else return NIL;
	}
	private LuaValue resolve(int i) {
		i--;
		if (i >= arr.length || i < 0) return error("out of range");
		return arr[i] == null ? NIL : arr[i];
	}

	@Override
	public void set(LuaValue value, LuaValue data) {
		if (value.isint())
			set(value.checkint(), data);
		else error("not integer");
	}

	@Override
	public void set(int i, LuaValue data) {
		i--;
		if (i >= arr.length || i < 0) {
			error("out of range");
			return;
		}
		arr[i] = data;
	}

	@Override
	public void set(int value, String data) {
		set(value, LuaString.valueOf(data));
	}
}
