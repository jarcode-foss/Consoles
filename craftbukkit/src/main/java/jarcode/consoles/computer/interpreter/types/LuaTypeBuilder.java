package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.interpreter.FunctionBind;
import jarcode.consoles.computer.interpreter.Lua;
import jarcode.consoles.computer.manual.Arg;
import jarcode.consoles.computer.manual.FunctionManual;
import jarcode.consoles.computer.manual.TypeManual;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@TypeManual("Used to build custom types for Lua programs.")
public class LuaTypeBuilder {

	private Function<String, LuaValue> stringGetHandler = null;
	private Function<Integer, LuaValue> integerGetHandler = null;
	private BiConsumer<String, LuaValue> stringSetHandler = null;
	private BiConsumer<Integer, LuaValue> integerSetHandler = null;
	private LuaValue constructor = null;


	@FunctionManual("Sets the handler function for when a value is being set at an " +
			"index. The first function parameter is the index, the second is the value.")
	public void stringSetHandler(
			@Arg(name = "bind", info = "the function to use for string value sets") FunctionBind bind) {
		stringSetHandler = (str, val) -> bind.call(str, val);
	}

	@FunctionManual("Sets the handler function for when a value is being set at an " +
			"index. The first function parameter is the index, the second is the value.")
	public void integerSetHandler(
			@Arg(name = "bind", info = "the function to use for integer value sets") FunctionBind bind) {
		integerSetHandler = (i, val) -> bind.call(i, val);
	}

	@FunctionManual("Sets the handler function for when the type is being indexed by " +
			"a string. The first argument of the passed function will be the string " +
			"being indexed with.")
	public void stringGetHandler(
			@Arg(name = "bind", info = "the function to use to handle string indexes") LuaValue bind) {
		stringGetHandler = (str) -> Lua.javaCallable(bind).call(str);
	}

	@FunctionManual("Sets the handler function for when the type is being indexed by " +
			"an integer. The first argument of the passed function will be the integer " +
			"being indexed with.")
	public void integerGetHandler(
			@Arg(name = "bind", info = "the function to use to handle integer indexes") LuaValue bind) {
		stringGetHandler = (i) -> Lua.javaCallable(bind).call(i);
	}

	@FunctionManual("Sets the construction handler for this type. When the supplying " +
			"function is called, the arguments are passed to the constructor in this " +
			"function.")
	public void constructor(
			@Arg(name = "constructor", info = "the function to invoke when instantiating this type") LuaValue value) {
		constructor = value;
	}

	@FunctionManual("Defines the type and returns a function that will return an " +
			"instance of the new type when called.")
	public LuaFunction define() {
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
				if (integerSetHandler != null)
					return integerGetHandler.apply(i);
				else return LuaValue.NIL;
			}

			private LuaValue resolve(String str) {
				if (stringGetHandler != null)
					return stringGetHandler.apply(str);
				else return LuaValue.NIL;
			}

			private void change(int i, LuaValue value) {
				if (integerSetHandler != null)
					integerSetHandler.accept(i, value);
			}

			private void change(String str, LuaValue value) {
				if (stringSetHandler != null)
					stringSetHandler.accept(str, value);
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
				if (constructor != null)
					constructor.invoke(varargs);
				return typeSupplier.get();
			}
		};
	}
}
