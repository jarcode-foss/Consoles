/*
  This abstract class provides a basis for native code to handle references closely
  tied with Java's garbage collection.
*/

public abstract class LuaNObject {

	// private, should never be accessed in Java code.

	// internal data for C structure
	private final byte[] __data;
	
	// stack of references
	private final Object[] __refs;

	// protected, should only be extended
	protected LuaNObject(long size, long referenceStackSize) {
		__data = new byte[size];
		__refs = new Object[referenceStackSize];
	}
}
