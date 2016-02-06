package ca.jarcode.ascript;

import ca.jarcode.ascript.interfaces.ScriptValue;
import ca.jarcode.consoles.computer.interpreter.GenericScriptError;
import com.google.gson.internal.LinkedTreeMap;

import java.util.*;

/*

Generics in Java generally ends up only being a compile-time feature. Sometimes we can rely
on type parameters being stored in bytecode, but it is still impossible to dynamically
'translate' arguments when calling a method with generic arguments. For example:


    public <T> void(T arg)

This ends up getting resolved to the following signature at compile-time:

	public void(Object arg)

This means when Joint tries to translate script values to Java to match the method signature,
it gets confused and tries to coerce everything as a Java object - this is very bad and will make
primitives (and strings, which may or may not be primitive depending on the language) get casted to
an object.

However, Java _can_ store type parameters for objects, for example:

    Map<String, Person> map = new HashMap<String, Person>(){};

This is actually a hack, and we abuse this hack using lambdas (and other libraries use it via TypeToken),
and it works because we are actually creating an instance of a map that extends HashMap with concrete types
String and Person, which exist after compile type in the class definition.

This allows us to resolve the type parameters of method references (via lambdas), but the member 'map' is still
of the abstract type Map<String, Person>, which we are still not introspect at runtime. We can only inspect the
instance and get the type parameters. To change this, we introduce this:

	@ConcreteDefinition
    HashMap<String, Person> map;

The annotation @ConcreteDefinition would have a compile-time agent that would expand the declaration of map to:

    static class _0 extends HashMap<String, Person> {} final _0 map = new _0();

Yes, this compiles.

This declares the type '_0', which extends the HashMap with its type arguments, and then declares map as
an instance of '_0', rather than HashMap. This allows us to introspect the field, without any instance, because
we can get the immediate type of 'map' and its type parameters.

We declare 'map' as final (and inherit other modifiers), because creating new instances of the map would have
to be an instance of _0, which would be impossible to replace at compile-time. We can, however, make great use
of this trick for deserializing java types when we encounter a field with generic types.

 */
public class JointDefaults {

	private static class map {

		public static boolean _RESTRICT = true;

		class WrappedMap<K, V> {
			final Map<K, V> map;
			final Class<K> key;
			final Class<V> value;
			public WrappedMap(Map<K, V> map, Class<K> key, Class<V> value) {
				this.map = map;
				this.key = key;
				this.value = value;
			}
			public void clear() {
				map.clear();
			}
			@SuppressWarnings("unchecked")
			public void put(ScriptValue key, ScriptValue value) {
				map.put(
						(K) Script.translateAndRelease(this.key, key),
						(V) Script.translateAndRelease(this.value, value)
				);
			}
			@SuppressWarnings({"unchecked", "SuspiciousMethodCalls"})
			public Object get(ScriptValue key) {
				return map.get(Script.translateAndRelease(this.key, key));
			}

			public int size() {
				return map.size();
			}

			public Map unwrap() {
				return map;
			}
		}

		@SuppressWarnings("unchecked")
		public WrappedMap wrap(Map map, Class keyType, Class valueType) {
			return new WrappedMap<>(map, keyType, valueType);
		}

		@SuppressWarnings("unchecked")
		public WrappedMap hashMap(Class keyType, Class valueType) {
			return new WrappedMap(new HashMap<>(), keyType, valueType);
		}

		@SuppressWarnings("unchecked")
		public WrappedMap linkedHashMap(Class keyType, Class valueType) {
			return new WrappedMap(new LinkedHashMap<>(), keyType, valueType);
		}

		@SuppressWarnings("unchecked")
		public WrappedMap treeMap(Class keyType, Class valueType) {
			return new WrappedMap(new TreeMap<>(), keyType, valueType);
		}

		@SuppressWarnings("unchecked")
		public WrappedMap linkedTreeMap(Class keyType, Class valueType) {
			return new WrappedMap(new LinkedTreeMap<>(), keyType, valueType);
		}

		@SuppressWarnings("unchecked")
		public WrappedMap newMap(Class<? extends Map> mapType, Class keyType, Class valueType) {
			try {
				return new WrappedMap(mapType.newInstance(), keyType, valueType);
			} catch (InstantiationException | IllegalAccessException e) {
				throw new Error(e);
			}
		}
	}

	private static class list {

		public static boolean _RESTRICT = true;

		class WrappedList<V> {
			final List<V> list;
			final Class<V> value;
			public WrappedList(List<V> list, Class<V> value) {
				this.list = list;
				this.value = value;
			}
			public void clear() {
				list.clear();
			}
			@SuppressWarnings("unchecked")
			public void set(int idx, ScriptValue element) {
				list.set(idx, (V) Script.translateAndRelease(value, element));
			}
			@SuppressWarnings("unchecked")
			public boolean add(ScriptValue element) {
				return list.add((V) Script.translateAndRelease(value, element));
			}
			public Object get(int index) {
				return list.get(index);
			}
			public int size() {
				return list.size();
			}
			public List unwrap() {
				return list;
			}
		}
	}

	// This gives scripts access to Java types directly

	private static class types {

		public static boolean _RESTRICT = true;

		public Class find(String path) {
			try {
				if (path.equals("int"))
					path = "integer";
				switch (path.toLowerCase()) {
					case "string":
					case "object":
					case "integer":
					case "double":
					case "float":
					case "boolean":
					case "long":
					case "short":
					case "byte":
						char[] arr = path.toCharArray();
						arr[0] = Character.toUpperCase(arr[0]);
						path = "java.lang." + new String(arr);
				}
				return Class.forName(path);
			} catch (ClassNotFoundException e) {
				throw new Error(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void load(String module) throws GenericScriptError {
		try {
			module = module.toLowerCase();
			Class type = Class.forName(JointDefaults.class.getName() + '$' + module);
			boolean restrict = type.getField("_RESTRICT").getBoolean(null);
			LibraryCreator.link(type, () -> {
				try {
					return type.newInstance();
				}
				catch (IllegalAccessException | InstantiationException e) {
					throw new GenericScriptError(e);
				}
			}, module, restrict);
		} catch (Exception e) {
			throw new GenericScriptError(e);
		}
	}
}
