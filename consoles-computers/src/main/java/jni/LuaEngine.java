package jni;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNInterface;
import ca.jarcode.consoles.computer.interpreter.luanative.LuaNScriptValue;

public class LuaEngine implements LuaNInterface {
	public native long setupinst(int impl, long heap, int interruptcheck);
	public native long unrestrict(long ptr);
	public native int destroyinst(long ptr);
	public native void setdebug(int mode);
	public native void setmaxtime(int time);
	public native LuaNScriptValue wrapglobals(long ptr);
	public native void free(ScriptValue value);
	public native ScriptValue load(long ptr, String value);
	public native void settable(long ptr, String table, String field, ScriptValue value);
	public native void kill(long ptr);
	public native void interruptreset(long ptr);
}
