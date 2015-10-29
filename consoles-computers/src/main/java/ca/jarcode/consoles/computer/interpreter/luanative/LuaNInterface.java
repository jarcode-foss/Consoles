package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;

// engine interface for lua and luajit
public interface LuaNInterface {
	// sets up a new interface
	long setupinst(int impl, long heap, int interruptcheck);
	// unrestricts an instance
	long unrestrict(long ptr);
	// destroys an instance
	int destroyinst(long ptr);
	// sets debug mode
	void setdebug(int mode);
	// wraps the lua globals into a script value
	LuaNScriptValue wrapglobals(long ptr);
	// frees resources from the C value (this makes the value unusable and should be out-of-scope)
	void free(ScriptValue value);
	// TODO: replace with direct file loading
	// loads a chunk from raw text
	ScriptValue load(long ptr, String value);
	// sets the value of a table
	void settable(long ptr, String table, String field, ScriptValue value);
	// kills the VM, and prevents any lua chunk/function from ever running in this VM
	// this uses a debug hooks that runs every n instructions and exits using setjmp/longjmp
	// this method of killing lua VMs is _much_ better than the LuaJ implementation
	void kill(long ptr);
	void interruptreset(long ptr);
	void setmaxtime(int time);
}
