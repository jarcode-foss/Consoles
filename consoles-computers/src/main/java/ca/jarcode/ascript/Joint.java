package ca.jarcode.ascript;

import ca.jarcode.ascript.interfaces.*;
import ca.jarcode.ascript.util.ThrowingConsumer;
import ca.jarcode.ascript.util.ThrowingFunction;
import ca.jarcode.ascript.util.ThrowingRunnable;
import ca.jarcode.ascript.util.ThrowingSupplier;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

// This class serves as the primary interface for Joint, a library that is (currently) embedded into Consoles.
// Eventually, this will be a standalone library with much more language support (Ruby, Python, Javascript).

public class Joint {

	public static int MAX_TIME_WITHOUT_INTERRUPT = 7000;
	public static boolean DEBUG_MODE = false;
	public static int INTERRUPT_CHECK_INTERVAL = 200;

	public static class Builder {
		ThrowingSupplier<Reader, IOException> readerFactory;
		boolean process = true;
		BooleanSupplier terminated = () -> false;
		InputStream stdin;
		OutputStream stdout;
		long heap = -1;
		Object userdata;
		ThrowingSupplier<JointType, EngineBuildException> typeSupplier;
		List<ThrowingConsumer<Joint, Exception>> postTasks = new ArrayList<>();
		Map<Class<?>, Object> searchClasses = new HashMap<>();
		String searchPrefix = "$";
		boolean useThreading;

		OutputStream findOut() {
			return new FileOutputStream(FileDescriptor.out);
		}

		InputStream findIn() {
			return new FileInputStream(FileDescriptor.in);
		}

		public Builder useThreading(boolean threading) {
			useThreading = threading;
			return this;
		}

		public Builder searchPrefix(String prefix) {
			this.searchPrefix = prefix;
			return this;
		}

		public Builder searchClass(Object instance) {
			searchClasses.put(instance.getClass(), instance);
			return this;
		}

		public Builder searchClass(Class<?> cls, Object instance) {
			searchClasses.put(cls, instance);
			return this;
		}

		public Builder source(String source) {
			return source(source, "<string>");
		}

		public Builder source(String source, String name) {
			processSource(source);
			postTasks.add((inst) -> inst.getUnsafe().addPreLoadedValue(
					inst.getUnsafe().getGlobals().load(source, name)
			));
			return this;
		}

		public Builder sourceFile(File file) {
			return sourceFile(file, file.getAbsolutePath());
		}

		public Builder sourceStream(InputStream stream) {
			return sourceStream(stream, "<stream>");
		}

		public Builder sourceStream(InputStream stream, String name) {
			processStream(stream);
			postTasks.add((inst) -> inst.getUnsafe().addPreLoadedValue(
					inst.getUnsafe().getGlobals().load(stream, name)
			));
			return this;
		}

		public Builder sourceFile(File file, String name) {
			processFile(file);
			postTasks.add((inst) -> inst.getUnsafe().addPreLoadedValue(
					inst.getUnsafe().getGlobals().load(file, name)
			));
			return this;
		}

		public Builder processSource(String source) {
			readerFactory = () -> new StringReader(source);
			return this;
		}

		public Builder processStream(InputStream stream) {
			readerFactory = () -> new InputStreamReader(stream);
			return this;
		}

		public Builder processFile(File file) {
			readerFactory = () -> new FileReader(file);
			typeSupplier = () -> JointType.getTypeForExtension(file.getName());
			return this;
		}

		public Builder engineType(JointType type) {
			typeSupplier = () -> type;
			return this;
		}

		public Builder preProcessScript(boolean process) {
			this.process = process;
			return this;
		}

		public Builder terminatedSupplier(BooleanSupplier terminated) {
			this.terminated = terminated;
			return this;
		}

		public Builder setSuggestedHeap(long heap) {
			this.heap = heap;
			return this;
		}

		public Builder contextUserdata(Object data) {
			userdata = data;
			return this;
		}

		public Joint build() throws EngineBuildException {
			if (stdin == null)
				stdin = findIn();
			if (stdout == null)
				stdout = findOut();
			if (readerFactory == null && process)
				throw new EngineBuildException("No method to read script");
			if (typeSupplier == null)
				throw new EngineBuildException("No engine type provided");
			JointType type = typeSupplier.get();
			JointType.Accessor accessor;
			try {
				accessor = process ? type.resolveAccessor(readerFactory.get()) : type.getDefaultAccessor();
			} catch (IOException e) {
				throw new EngineBuildException("Failed to read file", e);
			}
			if (accessor.THREAD_POLICY == JointType.ThreadPolicy.MULTITHREADED)
				useThreading = false;
			AtomicReference<ScriptGlobals> globalsRef = new AtomicReference<>();
			FuncPool<?> pool = new FuncPool<>(globalsRef::get, terminated, userdata);
			ThrowingFunction<JointThread, Joint, ScriptError> create = (thread) -> {
				pool.register(Thread.currentThread());
				ScriptGlobals globals = accessor.GETTER.newEnvironment(pool, terminated, stdin, stdout, heap);
				pool.mapStaticFunctions();
				for (Map.Entry<Class<?>, Object> entry : searchClasses.entrySet())
					Script.find(entry.getKey(), entry.getValue(), pool, searchPrefix);
				globals.load(pool);
				return new Joint(globals, pool, thread);
			};

			Joint instance;

			if (!useThreading) {
				instance = create.apply(null);
			}
			else {
				JointThread thread = new JointThread();
				thread.start();
				instance = thread.run(create, thread);
			}
			for (ThrowingConsumer<Joint, Exception> task : postTasks) {
				try {
					if (instance.THREAD != null) {
						instance.THREAD.runVoid(task, instance);
					}
					else task.accept(instance);
				} catch (Throwable e) {
					throw new EngineBuildException("Post task failed", e);
				}
			}
			return instance;
		}
	}

	private static class GenericScriptError extends ScriptError {
		Throwable cause;
		GenericScriptError(Throwable cause) {
			this.cause = cause;
		}

		@Override
		public Throwable underlying() {
			return getCause();
		}

		@Override
		public String constructMessage() {
			return getMessage();
		}

		@Override
		public Throwable getCause() {
			return cause;
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	private final ScriptGlobals G;
	private final FuncPool POOL;
	private final List<ScriptValue> LOADED = new ArrayList<>();
	private final JointThread THREAD;
	private final Unsafe UNSAFE = new Unsafe();

	public class Unsafe {
		public ScriptGlobals getGlobals() {
			return G;
		}
		public FuncPool getPool() {
			return POOL;
		}
		void addPreLoadedValue(ScriptValue value) {
			LOADED.add(value);
		}
	}

	private Joint(ScriptGlobals globals, FuncPool pool, JointThread thread) {
		this.G = globals;
		this.POOL = pool;
		this.THREAD = thread;
	}

	public <E extends Throwable> void runVoid(ThrowingRunnable<E> function) throws E {
		this.run(() -> {
				function.run();
				return null;
			});
	}

	public <R, E extends Throwable> R run(ThrowingSupplier<R, E> function) throws E {
		return THREAD == null ? function.get() : THREAD.run(function);
	}

	public Unsafe getUnsafe() {
		return UNSAFE;
	}
	public void detachAndDestroy() {
		runVoid(() -> {
				POOL.cleanup();
				G.close();
				if (THREAD != null) {
					G.cleanupThreadContext();
				}
			});
		if (THREAD != null) {
			THREAD.kill();
		}
	}
	public void notifyThreadEnd() {
		if (THREAD == null)
			G.cleanupThreadContext();
	}
	public void load(File file) throws IOException, ScriptError {
		load(file, file.getAbsolutePath());
	}
	public void load(File file, String path) throws IOException, ScriptError {
		runVoid(() -> {
				ScriptValue chunk = G.load(file, path);
				LOADED.add(chunk);
			});
	}
	public void load(String raw) throws ScriptError {
		load(raw, "<string>");
	}
	public void load(String raw, String path) throws ScriptError {
		runVoid(() -> {
				ScriptValue chunk = G.load(raw, path);
				LOADED.add(chunk);
			});
	}
	public void load(InputStream in) throws IOException, ScriptError {
		load(in, "<stream>");
	}
	public void load(InputStream in, String path) throws IOException, ScriptError {
		runVoid(() -> {
				ScriptValue chunk = G.load(in, path);
				LOADED.add(chunk);
			});
	}
	public <T> T loadValues(Class<T> type) {
		try {
			T instance = type.newInstance();
			loadValues(type, instance);
			return instance;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new GenericScriptError(e);
		}
	}
	public void loadValues(Object instance) {
		loadValues(instance.getClass(), instance);
	}
	public void loadValues(Class<?> type, Object instance) {
		runVoid(() -> {
				Class<?> typeAt = type;
				while (typeAt != Object.class) {
					for (Field field : typeAt.getDeclaredFields()) {
						int mod = field.getModifiers();
						if (!Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
							ScriptName annotation = field.getAnnotation(ScriptName.class);
							String name = annotation == null ? field.getName() : annotation.name();
							ScriptValue key = G.getValueFactory().translate(name, G);
							ScriptValue value = null;
							if (key != null) try {
									try {
										value = Script.translateToScriptValue(field.get(instance), G);
										if (value == null)
											value = G.getValueFactory().nullValue(G);
										G.set(key, value);
									} catch (IllegalAccessException e) {
										throw new GenericScriptError(e);
									}
								} finally {
									if (value != null)
										value.release();
									key.release();
								}
						}
					}
					typeAt = type.getSuperclass();
				}
			});
	}
	public void assignValues(Object instance) {
		assignValues(instance.getClass(), instance);
	}
	public void assignValues(Class<?> type, Object instance) throws ScriptError {
		runVoid(() -> {
				Class<?> typeAt = type;
				while (typeAt != Object.class) {
					for (Field field : typeAt.getDeclaredFields()) {
						int mod = field.getModifiers();
						if (!Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
							ScriptName annotation = field.getAnnotation(ScriptName.class);
							String name = annotation == null ? field.getName() : annotation.name();
							ScriptValue key = G.getValueFactory().translate(name, G);
							ScriptValue value = null;
							if (key != null) try {
									value = G.get(key);
									if (value != null && !value.isNull()) {
										field.setAccessible(true);
										try {
											field.set(instance,
													  Script.translateAndRelease(field.getType(), value));
										} catch (IllegalAccessException e) {
											throw new GenericScriptError(e);
										}
									}
								} finally {
									if (value != null)
										value.release();
									key.release();
								}
						}
					}
					typeAt = type.getSuperclass();
				}
			});
	}
	public void callLoaded(Object... args) {
		while (LOADED.size() > 0)
			call(Void.class, 0, args);
	}
	public int callStackSize() {
		return LOADED.size();
	}
	public <T> T callFirst(Class<T> expectedReturnType, Object... args) {
		return call(expectedReturnType, 0, args);
	}
	@SuppressWarnings("unchecked")
	public <T> T call(Class<T> expectedReturnType, int idx, Object... args) throws ScriptError {
		return run(() -> {
				if (LOADED.size() <= idx)
					throw new IllegalArgumentException("Invalid index");
				ScriptValue[] values = Script.translateToScriptValues(G, args);
				Object ret = Script.translateAndRelease(expectedReturnType,
														LOADED.get(0).getAsFunction()
														.call(values));
				for (ScriptValue value : values) {
					if (value != null)
						value.release();
				}
				LOADED.remove(0);
				return ret == null ? null : (T) ret;
			});
	}
	@SuppressWarnings("unchecked")
	public <T> T getGlobal(Class<T> expectedReturnType, String name) throws ScriptError {
		return run(() -> {
				ScriptValue nvalue = G.get(G.getValueFactory().translate(name, G));
				Object ret = Script.translateAndRelease(expectedReturnType, nvalue);
				nvalue.release();
				return ret == null ? null : (T) ret;
			});
	}
	public void setGlobal(String name, Object value) throws ScriptError {
		runVoid(() -> {
				ScriptValue kvalue = G.get(G.getValueFactory().translate(name, G));
				ScriptValue nvalue = Script.translateToScriptValue(value, G);
				G.set(kvalue, nvalue);
				kvalue.release();
				if (nvalue != null)
					nvalue.release();
			});
	}
	public void loadLibrary(ScriptLibrary library) throws ScriptError {
		runVoid(() -> G.load(library));
	}
	public JointThread getThread() {
		return THREAD;
	}
}
