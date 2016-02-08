package ca.jarcode.ascript.luanative;

import ca.jarcode.ascript.interfaces.ScriptFunction;
import ca.jarcode.ascript.interfaces.ScriptValue;

import java.util.*;

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
	 */
	public static int releaseRemainingContextValues(long address) {
		if (TRACK_INSTANCES) {
			HashMap<Long, LuaNScriptValue> map = TRACKED.get();
			int n = 0;
			for (LuaNScriptValue value : map.values()) {
				if (value.instAddress() == address) {
					value.release0();
					++n;
				}
			}
			map.clear();
			return n;
		}
		return -1;
	}
	
	public long __address;

	{
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
	
	public void release() {
		if (TRACK_INSTANCES && __address != 0) {
			TRACKED.get().remove(__address);
		}
		__address = 0;
		release0();
	}
	
	// internal release
	private native void release0();
	// engine_inst* address
	private native long instAddress();

}
