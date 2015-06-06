package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.interpreter.FunctionBind;
import jarcode.consoles.computer.interpreter.Lua;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LuaTypeBuilder {
	private Function<String, LuaValue> stringGetHandler = null;
	private Function<Integer, LuaValue> integerGetHandler = null;
	private BiConsumer<String, LuaValue> stringSetHandler = null;
	private BiConsumer<Integer, LuaValue> integerSetHandler = null;
	private LuaValue constructor = null;

	public void stringSetHandler(FunctionBind bind) {
		stringSetHandler = (str, val) -> bind.call(str, val);
	}

	public void integerSetHandler(FunctionBind bind) {
		integerSetHandler = (i, val) -> bind.call(i, val);
	}

	public void stringGetHandler(LuaValue bind) {
		stringGetHandler = (str) -> Lua.javaCallable(bind).call(str);
	}

	public void integerGetHandler(LuaValue bind) {
		stringGetHandler = (i) -> Lua.javaCallable(bind).call(i);
	}

	public void constructor(LuaValue value) {
		constructor = value;
	}

	public static LuaFunction define(LuaTypeBuilder builder) {
		Supplier<LuaValue> typeSupplier = () -> new LuaValue() {
			@Override
			public int type() {
				return 110;
			}

			@Override
			public String typename() {
				return "user_type";
			}

			@Override
			public LuaValue get(LuaValue value) {
				if (value.isint())
					return resolve(value.checkint());
				else if (value.isstring())
					return resolve(value.checkjstring());
				else return error("invalid");
			}
			@Override
			public LuaValue get(int i) {
				return resolve(i);
			}
			@Override
			public LuaValue get(String str) {
				return resolve(str);
			}
			private LuaValue resolve(int i) {
				if (builder.integerSetHandler != null)
					return builder.integerGetHandler.apply(i);
				else return LuaValue.NIL;
			}

			private LuaValue resolve(String str) {
				if (builder.stringGetHandler != null)
					return builder.stringGetHandler.apply(str);
				else return LuaValue.NIL;
			}

			private void change(int i, LuaValue value) {
				if (builder.integerSetHandler != null)
					builder.integerSetHandler.accept(i, value);
			}

			private void change(String str, LuaValue value) {
				if (builder.stringSetHandler != null)
					builder.stringSetHandler.accept(str, value);
			}

			@Override
			public void set(LuaValue value, LuaValue data) {
				if (value.isint())
					change(value.checkint(), data);
				else if (value.isstring())
					change(value.checkjstring(), data);
				else error("invalid");

			}

			@Override
			public void set(int i, LuaValue data) {
				change(i, data);
			}

			@Override
			public void set(int value, String data) {
				change(value, LuaString.valueOf(data));
			}

			@Override
			public void set(String value, LuaValue data) {
				change(value, data);
			}

			@Override
			public void set(String value, String data) {
				change(value, LuaString.valueOf(data));
			}
		};
		return new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs varargs) {
				if (builder.constructor != null)
					builder.constructor.invoke(varargs);
				return typeSupplier.get();
			}
		};
	}
}
