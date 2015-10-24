package jni;

import ca.jarcode.consoles.computer.NativeLoader;

public class LibLoader implements NativeLoader.ILoader {
	public native long dlopen(String name);
	public native int dlclose(long handle);
}
