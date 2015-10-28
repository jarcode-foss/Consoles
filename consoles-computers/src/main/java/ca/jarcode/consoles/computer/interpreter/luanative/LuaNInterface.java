package ca.jarcode.consoles.computer.interpreter.luanative;

// engine interface for lua and luajit
public interface LuaNInterface {
	long setupinst(int impl, long heap);
	long unrestrict(long ptr);
	int destroyinst(long ptr);
	void setdebug(int mode);
	LuaNScriptValue wrapglobals(long ptr);
}
