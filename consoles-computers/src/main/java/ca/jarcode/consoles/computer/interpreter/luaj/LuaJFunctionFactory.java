package ca.jarcode.consoles.computer.interpreter.luaj;

import ca.jarcode.consoles.computer.interpreter.Lua;
import ca.jarcode.consoles.computer.interpreter.interfaces.FunctionFactory;
import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptFunction;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LuaJFunctionFactory implements FunctionFactory {

	@Override
	public ScriptFunction createFunction(Class[] types, Object func) {
		return new LuaJScriptFunction(prepFunction(types, func));
	}

	public LibFunction prepFunction(Class[] types, Object func) {
		if (types.length == 1)
			return new OneArgFunction() {

				@Override
				public LuaValue call(LuaValue v1) {
					return ((LuaJScriptValue) Lua.translateToScriptValue(Lua.callAndRelease(func, types,
							new LuaJScriptValue(v1)
					))).val;
				}
			};
		else if (types.length == 2)
			return new TwoArgFunction() {

				@Override
				public LuaValue call(LuaValue v1, LuaValue v2) {
					return ((LuaJScriptValue) Lua.translateToScriptValue(Lua.callAndRelease(func, types,
							new LuaJScriptValue(v1),
							new LuaJScriptValue(v2)
					))).val;
				}
			};
		else if (types.length == 3)
			return new ThreeArgFunction() {

				@Override
				public LuaValue call(LuaValue v1, LuaValue v2, LuaValue v3) {
					return ((LuaJScriptValue) Lua.translateToScriptValue(Lua.callAndRelease(func, types,
							new LuaJScriptValue(v1),
							new LuaJScriptValue(v2),
							new LuaJScriptValue(v3)
					))).val;
				}
			};
		else if (types.length == 0)
			return new ZeroArgFunction() {
				@Override
				public LuaValue call() {
					return ((LuaJScriptValue) Lua.translateToScriptValue(Lua.callAndRelease(func, types))).val;
				}
			};

		return new LibFunction() {
			@Override
			public LuaValue call() {
				return ((LuaJScriptValue) Lua.translateToScriptValue(Lua.callAndRelease(func, types))).val;
			}

			@Override
			public LuaValue call(LuaValue v1) {
				return ((LuaJScriptValue) Lua.translateToScriptValue(Lua.callAndRelease(func, types,
						new LuaJScriptValue(v1)
				))).val;
			}

			@Override
			public LuaValue call(LuaValue v1, LuaValue v2) {
				return ((LuaJScriptValue) Lua.translateToScriptValue(Lua.callAndRelease(func, types,
						new LuaJScriptValue(v1),
						new LuaJScriptValue(v2)
				))).val;
			}

			@Override
			public LuaValue call(LuaValue v1, LuaValue v2, LuaValue v3) {
				return ((LuaJScriptValue) Lua.translateToScriptValue(Lua.callAndRelease(func, types,
						new LuaJScriptValue(v1),
						new LuaJScriptValue(v2),
						new LuaJScriptValue(v3)
				))).val;
			}

			@Override
			public LuaValue call(LuaValue v1, LuaValue v2, LuaValue v3, LuaValue v4) {
				return ((LuaJScriptValue) Lua.translateToScriptValue(Lua.callAndRelease(func, types,
						new LuaJScriptValue(v1),
						new LuaJScriptValue(v2),
						new LuaJScriptValue(v3),
						new LuaJScriptValue(v4)
				))).val;
			}

			@Override
			public LuaValue invoke(Varargs value) {
				Object[] total = new Object[types.length];
				for (int t = 0; t < types.length; t++) {
					total[t] = Lua.translateAndRelease(types[t], new LuaJScriptValue(value.arg(t)));
				}
				return ((LuaJScriptValue) Lua.translateToScriptValue(Lua.callAndRelease(func, types, total))).val;
			}
		};
	}

	@Override
	public ScriptFunction createFunction(Method m, Object inst) {
		Class[] types = m.getParameterTypes();
		return new LuaJScriptFunction(new LibFunction() {
			@Override
			public LuaValue call() {
				try {
					return ((LuaJScriptValue) Lua.translateToScriptValue(m.invoke(inst)))
							.val;
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new LuaError(e);
				}
			}

			@Override
			public LuaValue call(LuaValue v) {
				try {
					return ((LuaJScriptValue) Lua.translateToScriptValue(m.invoke(inst, Lua.toJavaAndRelease(types,
							new LuaJScriptValue(v)
					)))).val;
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new LuaError(e);
				}
			}

			@Override
			public LuaValue call(LuaValue v, LuaValue v1) {
				try {
					return ((LuaJScriptValue) Lua.translateToScriptValue(m.invoke(inst, Lua.toJavaAndRelease(types,
							new LuaJScriptValue(v),
							new LuaJScriptValue(v1)
					)))).val;
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new LuaError(e);
				}
			}

			@Override
			public LuaValue call(LuaValue v, LuaValue v1, LuaValue v2) {
				try {
					return ((LuaJScriptValue) Lua.translateToScriptValue(m.invoke(inst, Lua.toJavaAndRelease(types,
							new LuaJScriptValue(v),
							new LuaJScriptValue(v1),
							new LuaJScriptValue(v2)
					)))).val;
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new LuaError(e);
				}
			}

			@Override
			public LuaValue call(LuaValue v, LuaValue v1, LuaValue v2, LuaValue v3) {
				try {
					return ((LuaJScriptValue) Lua.translateToScriptValue(m.invoke(inst, Lua.toJavaAndRelease(types,
							new LuaJScriptValue(v),
							new LuaJScriptValue(v1),
							new LuaJScriptValue(v2),
							new LuaJScriptValue(v3)
					)))).val;
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new LuaError(e);
				}
			}
		});
	}
}
