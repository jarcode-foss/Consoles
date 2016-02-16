package ca.jarcode.ascript.luanative;

import ca.jarcode.ascript.interfaces.ScriptFunction;
import ca.jarcode.ascript.interfaces.ScriptValue;

import java.util.*;
import java.util.stream.*;

public class LuaNScriptValue implements ScriptValue, ScriptFunction {

	/*
	  This is a really special class, some properties include:
	  
	      - this class has it's own members, but they are implemented in C
		  - this class has no constructor, but it still needs to be assigned data on creation only visible from C
		  - instances of this class are only safely created created via the function factory (also native)
		  - unlike the LuaJ implementation, a value and a function are the same instance
		  - unlike the LuaJ implementation, a script value is a copy of lua or java data, not a wrapper
	*/

	/*
	  This is a replacement for the old tracking system. It can be completely disabled, but
	  it's important for profiling and keeping track of memory leaks. It's very-low overhead.
	*/

	public static final boolean TRACK_INSTANCES = true; /* set to false to disable */

	/*
	  This is thread-specific data, we use it to avoid synchronization entirely.
	*/
	public static final LuaNThreadDatum<HashMap<Long, LuaNScriptValue>> TRACKED = new LuaNThreadDatum<>();

	/*
	  This is basically manual garbage collection for this class, values should be
	  released using release(), but if there are remaining values and TRACK_INSTANCES
	  is set to true, then we can check to see if there's any remaining values.
	  
	  If this method returns a value greater than zero, it indicates a memory leak
	  which would grow if this collection was disabled.
	  
	  Idealy, this feature should be disabled, and no leaks should ever occur.
	*/
	public static int releaseRemainingContextValues(long address) {
		HashMap<Long, LuaNScriptValue> map = TRACKED.get();
		int n = 0;
		Iterator<Map.Entry<Long, LuaNScriptValue>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, LuaNScriptValue> entry = it.next();
			LuaNScriptValue value = entry.getValue();
			if (value.instAddress() == address) {
				value.release0();
				it.remove();
				++n;
			}
		}
		return n;
	}

	/*
	  This is for debugging, mostly. For any given engine_inst* address, you
	  can obtain tracked engine values that have not been released. A null
	  address (0) will return all values that are not associated with an
	  engine.
	  
	  This only returns values for the current thread.
	*/
	public static LuaNScriptValue[] remainingContextValues(long address) {
		HashMap<Long, LuaNScriptValue> map = TRACKED.get();
		return map.values().stream()
			.filter((value) -> value.instAddress() == address)
			.toArray(LuaNScriptValue[]::new);
	}

	/*
	  This is a 32 or 64 bit address that points to the implementaion of this class
	  
	  This value is volatile to prevent multiple threads having a window to release
	  a value at the same time (causing a double-free).
	*/
	private volatile long __address;

	/*
	  this constructor is private, but it's called from native code.
	*/
	private LuaNScriptValue(long address) {
		this.__address = address;
		if (TRACK_INSTANCES) {
			TRACKED.get().put(__address, this);
		}
	}

	public native Object translateObj();
	public native boolean canTranslateObj();
	public native String translateString();
	public native boolean canTranslateString();
	public native long translateLong();
	public native boolean canTranslateLong();
	public native short translateShort();
	public native boolean canTranslateShort();
	public native byte translateByte();
	public native boolean canTranslateByte();
	public native int translateInt();
	public native boolean canTranslateInt();
	public native float translateFloat();
	public native boolean canTranslateFloat();
	public native double translateDouble();
	public native boolean canTranslateDouble();
	public native boolean translateBoolean();
	public native boolean canTranslateBoolean();
	public native boolean isNull();
	public native boolean canTranslateArray();
	public native Object translateArray(Class arrClass);
	public native boolean isFunction();
	public native void set(ScriptValue key, ScriptValue value);
	public native ScriptValue get(ScriptValue key);
	public native ScriptValue call();
	public native ScriptValue copy();
	public native ScriptValue call(ScriptValue... args);

	// this can be a value or a function, so just return the same object
	public ScriptValue getAsValue() { return this; }
	public ScriptFunction getAsFunction() { return this; }

	/*
	  LuaN's engine_value type will always hold a global reference to its
	  associated Java object, preventing garbage collection from ever
	  happening. When we release the value, that reference (along with
	  other internal global refs) is deleted, and the associated java
	  object can then be marked by the VM for collection.
	*/
	public void release() {
		if (__address != 0) {
			if (TRACK_INSTANCES) {
				if (TRACKED.get().remove(__address) == null) {
					/*
					  If this happens, release() is being called in a thread that
					  a script value wasn't created in.
					*/
					throw new LuaNError("value is untracked, but has a non-null address");
				}
			}
			release0();
			__address = 0;
		}
		else throw new LuaNError("value already released");
	}
	
	// internal release
	private native void release0();
	// engine_inst* address
	private native long instAddress();

}
