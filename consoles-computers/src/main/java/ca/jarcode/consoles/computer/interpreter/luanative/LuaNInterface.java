package ca.jarcode.consoles.computer.interpreter.luanative;

import ca.jarcode.consoles.computer.interpreter.interfaces.ScriptValue;

// engine interface for lua and luajit
public interface LuaNInterface {
	// utility method for naming the current thread for debugging purposes.
	// this method singlehandely breaks Windows compatibility
	void pthread_name(String name);
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
	// TODO: replace with direct file loading
	// loads a chunk from raw text
	ScriptValue load(long ptr, String value, String path);
	// sets the value of a table
	void settable(long ptr, String table, String field, ScriptValue value);
	// kills the VM, and prevents any lua chunk/function from ever running in this VM
	// this uses a debug hooks that runs every n instructions and exits using setjmp/longjmp
	// this method of killing lua VMs is _much_ better than the LuaJ implementation
	void kill(long ptr);
	// sets the interrupt timer
	void interruptreset(long ptr);
	// sets the maximum time before an instance is interrupted
	void setmaxtime(int time);
	// setup the native interface (called during install)
	void setup();
	// blacklist all functions that are never intended to be used
	void blacklist(long ptr);
}
