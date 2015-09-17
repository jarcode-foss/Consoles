/*
 * Cloning and reference tools for encouraging bad programming habits.
 */
package ca.jarcode.consoles.util.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Unsafe tools for unsafe programming. Have fun!
 *
 * ONLY TESTED WITH THE SUN JVM
 *
 * Most of the operations this class performs are ridiculously slow. I don't
 * know why you would need to use them. They are terrible practise to use anyway.
 *
 * @author Jarcode
 */
@SuppressWarnings("unused")
public class UnsafeTools {
	// Used for keeping track of objects that aren't being tracked by the JVM.
	private static final ArrayList<Object> UNMANAGED = new ArrayList<>();
	public static final short JVM_ARCH = (short) (System.getProperty("sun.arch.data.model").contains("64") ? 64 : 32);
	/**
	 * Returns all objects that have not been destroyed that are not tracked by the
	 * JVM. It is not recommended to retrieve objects that need to be destroyed via
	 * this method.
	 *
	 * @return an array of all objects not tracked by the JVM.
	 */
	public synchronized static Object[] getUnmanagedObjects() {
		return UNMANAGED.toArray(new Object[UNMANAGED.size()]);
	}
	/**
	 * Returns the amount of memory that has not been destroyed that is not tracked by the
	 * JVM.
	 *
	 * @return amount of data (in bytes) that is not tracked by the JVM.
	 */
	public synchronized static long getUnmanagedMemory() {
		long data = 0;
		for (Object obj : UNMANAGED) {
			data += sizeOf(obj);
		}
		return data;
	}
	/**
	 * Creates an exact copy of a given object that is not managed by the JVM,
	 * so the object will not be collected if it is unreferenced.<br><br>
	 *
	 * The copy of the object contains the same references as the source object,
	 * so this method is not ideal to use to create a clone of an object.<br><br>
	 *
	 * To destroy the copied object, use the {@link ca.jarcode.consoles.util.unsafe.UnsafeTools#destroy(Object)}.
	 *
	 * @param <T> the object type
	 * @param obj  object to copy
	 * @return  unmanaged copy of the object
	 */
	@SuppressWarnings("unchecked")
	public static <T> T unmanagedCopy(T obj) {
		if (obj == null) return null;
		return unmanagedCopy(obj, (Class<T>) obj.getClass());
	}
	@SuppressWarnings("unchecked")
	private synchronized static <T> T unmanagedCopy(T obj, Class<T> type) {
		Unsafe unsafe = getUnsafe();
		long size = sizeOf(obj, type);
		long start = toAddress(obj);
		long address = unsafe.allocateMemory(size);
		unsafe.copyMemory(start, address, size);
		T unmanaged = (T) fromAddress(address);
		UNMANAGED.add(unmanaged);
		return unmanaged;
	}
	/**
	 * Creates a unique copy of a given object that is not manage by the JVM,
	 * so the object will not be collected if it is unreferenced.<br><br>
	 *
	 * The copy of the object also contains references to unique copies of unmanaged
	 * objects. This method is ideal for creating unmanaged objects that recursively
	 * create off-heap copies of its containing objects.<br><br>
	 *
	 * To destroy the copied object, use the {@link ca.jarcode.consoles.util.unsafe.UnsafeTools#destroy(Object)}.
	 *
	 * @param <T> the object type
	 * @param obj  object to copy
	 * @param ignore  list of objects to directly copy references with
	 * @return unmanaged unique copy of the object
	 */
	public static <T> T unmanagedClone(T obj, Object... ignore) {
		return unmanagedCopy(copyObject(obj, obj.getClass(), UnsafeTools::unmanagedCopy, null, new HashMap<>(), ignore));
	}
	/**
	 * Destroys the specified object that is not managed by JVM. Remaining references
	 * to the object are left pointing to the non-existent object, but the references
	 * will not be set to null.<br><br>
	 *
	 * Remaining references have undefined behavior after the object has been destroyed.
	 *
	 * @param obj the object to collect
	 * @throws RuntimeException on invalid argument.
	 */
	public synchronized static void destroy(Object obj) {
		if (!containsReference(UNMANAGED, obj)) {
			throw new RuntimeException("Tried to manually destroy object managed by the JVM");
		}
		try {
			Method method = Object.class.getDeclaredMethod("finalize");
			method.setAccessible(true);
			method.invoke(obj);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		Unsafe unsafe = getUnsafe();
		UNMANAGED.remove(obj); // It would make sense to remove it after, but you get an NPE thrown when you use a faulty reference.
		unsafe.freeMemory(toAddress(obj));
	}
	/**
	 * Throws an exception that does not need to be caught during compile time.<br><br>
	 *
	 * This is a safer and better alternative to {@link Thread#stop(Throwable)}
	 *
	 * @param e the exception to throw
	 */
	public static void throwException(Throwable e) {
		getUnsafe().throwException(e);
	}
	/*
	 * Do not use toAddress or fromAddress to store references to objects, as
	 * the address of an object can dynamically change based on JVM implementation
	 */
	private static long toAddress(Object obj) {
		Unsafe unsafe = getUnsafe();
		Object[] array = new Object[] {obj};
		long baseOffset = getUnsafe().arrayBaseOffset(Object[].class);
		return normalize(unsafe.getInt(array, baseOffset));
	}
	private static Object fromAddress(long address) {
		Object[] array = new Object[]{null};
		long baseOffset = getUnsafe().arrayBaseOffset(Object[].class);
		getUnsafe().putLong(array, baseOffset, address);
		return array[0];
	}
	/**
	 * Returns the size of the type that is passed to this method. Primitive
	 * types return their according size, in bytes, and objects return four bytes
	 * (for the reference)
	 *
	 * @param type the type to query
	 * @return size of the primitive type, or four for an object reference, in bytes.
	 */
	public static long componentSize(Class<?> type) {
		if (!type.isPrimitive()) {
			switch (JVM_ARCH) {
				case 32: return 4;
				case 64: return 8;
			}
		}
		int componentSize;
		if (type == boolean.class || type == byte.class) {
			componentSize = 1;
		} else if (type == char.class || type == short.class) {
			componentSize = 2;
		} else if (type == int.class || type == float.class) {
			componentSize = 4;
		} else if (type == long.class || type == double.class) {
			componentSize = 8;
		} else {
			componentSize = -1;
		}
		return componentSize;
	}
	/**
	 * Returns the size of the underlying object and all the objects that containing
	 * references point to, recursively.
	 *
	 * @param obj reference to the object
	 * @return size of all underlying and related data
	 */
	public static long sizeOfAll(Object obj) {
		long size = sizeOf(obj);
		for (Map.Entry<Object, Class<?>> entry : getReferences(obj)) {
			Object ref = entry.getKey();
			if (ref != null) {
				size += sizeOf(ref);
			}
		}
		return size;
	}
	/**
	 * Returns a list of references tied to the underlying object passed to this
	 * method. Duplicate and null references are omitted.
	 *
	 * @param obj reference to the object
	 * @return array of all related references
	 */
	public static List<Map.Entry<Object, Class<?>>> getReferences(Object obj) {
		return getReferences(obj, new ArrayList<>());
	}
	private static List<Map.Entry<Object, Class<?>>> getReferences(Object obj, ArrayList<Object> added) {

		if (obj == null) return new ArrayList<>();

		ArrayList<Map.Entry<Object, Class<?>>> list = new ArrayList<>();

		try {
			if (obj.getClass().isArray()) {
				Class<?> type = obj.getClass().getComponentType();

				if (type.isPrimitive()) return new ArrayList<>();

				for (int t = 0; t < Array.getLength(obj); t++) {
					Object value = Array.get(obj, t);
					if (value != null && !containsReference(added, value)) {
						added.add(value);
						list.addAll(getReferences(value, added));
						list.add(new AbstractMap.SimpleEntry<>(value, type));
					}
				}
			}
			else {
				for (Field field : fields(obj.getClass())) {
					if (!field.getType().isPrimitive()) {

						field.setAccessible(true);

						if (!field.getName().startsWith("this$")) {
							Object ref = field.get(obj);
							if (ref != null && !containsReference(added, ref)) {
								added.add(ref);
								list.addAll(getReferences(ref, added));
								list.add(new AbstractMap.SimpleEntry<>(ref, field.getType()));
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	private static <T> boolean containsReference(T[] array, Object reference) {
		return containsReference(Arrays.asList(array), reference);
	}
	private static boolean containsReference(List list, Object reference) {
		for (Object obj : list) {
			if (obj == reference) {
				return true;
			}
		}
		return false;
	}
	/**
	 * Returns the size of the underlying object behind the reference passed to this method.
	 *
	 * @param obj reference to the object to query
	 * @return size of the underlying object
	 */
	public static long sizeOf(Object obj) {
		if (obj == null) return 0;
		return sizeOf(obj, obj.getClass());
	}
	private static long sizeOf(Object obj, Class<?> type) {
		Unsafe unsafe = getUnsafe();
		if (type.isArray()) {
			long componentSize = componentSize(obj.getClass().getComponentType());
			return 12L + (componentSize * Array.getLength(obj));
		}
		return unsafe.getAddress(normalize(unsafe.getInt(obj, 4L)) + 12L);
	}
	private static long normalize(int value) {
		if (value >= 0) return value;
		return (~0L >>> 32) & value;
	}
	/**
	 * Creates a unique clone of the object passed to this method. All contained
	 * objects are also cloned, recursively, to prevent the object containing
	 * the same references as the source object. Equal references within the
	 * source object will have new equal references in the returned object.<br><br>
	 *
	 * Objects added to the ignore list are not copied over in the returned object
	 * and are instead assigned to the corresponding fields with the same reference.<br><br>
	 *
	 * The newly created object does not have any constructor called upon creation.<br><br>
	 *
	 * It is recommended to avoid using this method when possible in favor of using the
	 * {@link Object#clone()} method.<br><br>
	 *
	 * @param <T> type of getInstance
	 * @param instance getInstance of the object to clone
	 * @param ignore list of objects to directly copy references with
	 * @return a unique clone of the object.
	 */
	public static <T> T clone(T instance, Object... ignore) {
		return copyObject(instance, instance.getClass(), in -> in, null, new HashMap<>(), ignore);
	}
	/**
	 * Returns the getInstance of the {@link sun.misc.Unsafe} class
	 *
	 * @return the getInstance of the {@link sun.misc.Unsafe} class
	 */
	public static Unsafe getUnsafe() {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			return (Unsafe) field.get(null);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private static List<Field> fields(Class<?> type) {
		ArrayList<Field> fields = new ArrayList<>();
		while (type != Object.class) {
			fields.addAll(Arrays.asList(type.getDeclaredFields()));
			type = type.getSuperclass();
		}
		return fields;
	}
	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> type) throws InstantiationException {
		Unsafe unsafe = getUnsafe();
		return (T) unsafe.allocateInstance(type.getClass());
	}
	@SuppressWarnings("unchecked")
	private static <T> T copyObject(T instance, Class<?> type, NestedObjectCloner cloner, Object inner, HashMap<Object, Object> map, Object[] ignore) {
		if (instance == null) {
			return null;
		}
		Unsafe unsafe = getUnsafe();
		try {
			T newInstance = (T) unsafe.allocateInstance(instance.getClass());
			for (Field field : fields(instance.getClass())) {
				if (field.getType() == Class.forName("java.lang.reflect.ReflectAccess")) {
					System.out.println("reflect access class");
				}
				if (!field.getName().startsWith("this$")) { // parent class declarations from inner classes
					handleField(field, cloner, newInstance, instance, map, ignore);
				}
				else {
					field.setAccessible(true);
					field.set(newInstance, inner); // assign parent class
				}
			}
			return newInstance;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Allocates a new object in the java heap
	 *
	 * @param type the class to allocate
	 * @param <T> class type
	 * @return an instance of the class
	 * @throws InstantiationException if the class could not be instantiated
	 * @see {@link sun.misc.Unsafe#allocateInstance}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T allocateInstance(Class<T> type) throws InstantiationException {
		Unsafe unsafe = getUnsafe();
		return  (T) unsafe.allocateInstance(type);
	}
	private static void handleField(Field field, NestedObjectCloner cloner, Object newInstance, Object instance, HashMap<Object, Object> map, Object[] ignore) throws IllegalAccessException {

		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		if (!Modifier.isStatic(field.getModifiers())) {
			if (field.getType().isPrimitive()) {
				if (field.getType() == int.class) {
					field.setInt(newInstance, field.getInt(instance));
				} else if (field.getType() == short.class) {
					field.setShort(newInstance, field.getShort(instance));
				} else if (field.getType() == long.class) {
					field.setLong(newInstance, field.getLong(instance));
				} else if (field.getType() == double.class) {
					field.setDouble(newInstance, field.getDouble(instance));
				} else if (field.getType() == float.class) {
					field.setFloat(newInstance, field.getFloat(instance));
				} else if (field.getType() == char.class) {
					field.setChar(newInstance, field.getChar(instance));
				} else if (field.getType() == byte.class) {
					field.setByte(newInstance, field.getByte(instance));
				} else if (field.getType() == boolean.class) {
					field.setBoolean(newInstance, field.getBoolean(instance));
				}
			} else if (field.getType().isArray()) {
				Object obj = field.get(instance);
				if (containsReference(ignore, obj)) {
					field.set(newInstance, obj);
				}
				else {
					Object array;
					if (map.containsKey(obj)) {
						array = map.get(obj);
					} else {
						array = cloner.clone(copyArray(field.get(instance), cloner, map, ignore));
						map.put(obj, array);
					}
					field.set(newInstance, array);
				}
			} else {
				Object obj = field.get(instance);
				if (containsReference(ignore, obj)) {
					field.set(newInstance, obj);
				}
				else {
					if (map.containsKey(obj)) {
						field.set(newInstance, map.get(obj));
					} else {
						Object copy = cloner.clone(copyObject(obj, field.getType(), cloner, newInstance, map, ignore));
						field.set(newInstance, copy);
						map.put(obj, copy);
					}
				}
			}
		}
	}
	private static Object copyArray(Object old, NestedObjectCloner cloner, HashMap<Object, Object> map, Object[] ignore) throws IllegalAccessException {
		Class<?> type = old.getClass().getComponentType();
		int len = Array.getLength(old);
		Object array = Array.newInstance(type, len);
		for (int t = 0; t < len; t++) {
			if (type == int.class) {
				Array.setInt(array, t, Array.getInt(old, t));
			} else if (type == short.class) {
				Array.setShort(array, t, Array.getShort(old, t));
			} else if (type == long.class) {
				Array.setLong(array, t, Array.getLong(old, t));
			} else if (type == double.class) {
				Array.setDouble(array, t, Array.getDouble(old, t));
			} else if (type == float.class) {
				Array.setFloat(array, t, Array.getFloat(old, t));
			} else if (type == char.class) {
				Array.setChar(array, t, Array.getChar(old, t));
			} else if (type == byte.class) {
				Array.setByte(array, t, Array.getByte(old, t));
			} else if (type == boolean.class) {
				Array.setBoolean(array, t, Array.getBoolean(old, t));
			} else if (type.isArray()) {
				Object obj = Array.get(old, t);

				if (containsReference(ignore, obj)) {
					Array.set(array, t, obj);
				}
				else {
					if (map.containsKey(obj)) {
						Array.set(array, t, map.get(obj));
					} else {
						Object copy = cloner.clone(copyArray(obj, cloner, map, ignore));
						Array.set(array, t, copy);
						map.put(obj, copy);
					}
				}
			}
			else {
				Object obj = Array.get(old, t);

				if (containsReference(ignore, obj)) {
					Array.set(array, t, obj);
				}
				else {
					if (map.containsKey(obj)) {
						Array.set(array, t, map.get(obj));
					} else {
						Object copy = cloner.clone(copyObject(obj, type, cloner, null, map, ignore));
						Array.set(array, t, copy);
						map.put(obj, copy);
					}
				}
			}
		}
		return array;
	}

	private interface NestedObjectCloner {
		Object clone(Object in);
	}
}

