package jni;

public class NLoader {
	public native long dlopen(String name);
	public native int dlclose(long handle);
}
