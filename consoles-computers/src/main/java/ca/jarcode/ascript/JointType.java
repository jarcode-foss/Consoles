package ca.jarcode.ascript;

import ca.jarcode.ascript.interfaces.FuncPool;
import ca.jarcode.ascript.interfaces.ScriptGlobals;
import ca.jarcode.ascript.luaj.LuaJEngine;
import ca.jarcode.ascript.luanative.LuaNEngine;
import ca.jarcode.ascript.luanative.LuaNImpl;
import ca.jarcode.consoles.computer.CompileTarget;
import ca.jarcode.consoles.computer.NativeLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.function.BooleanSupplier;

import static ca.jarcode.ascript.JointType.ThreadPolicy.*;
import static ca.jarcode.ascript.JointType.Policy.*;
import static ca.jarcode.consoles.computer.CompileTarget.*;

public enum JointType {
	LUA(".lua", "--!", PREFER_NATIVE,
		engine("luaj", LuaJEngine::install,
			   LuaJEngine::newEnvironment,
			   THREAD_SENSITIVE, JAVA),
		engine("luajit", () -> LuaNEngine.install(LuaNImpl.JIT),
			   LuaNEngine::newEnvironment,
			   THREAD_SENSITIVE, ELF32, ELF64),
		engine("lua", () -> LuaNEngine.install(LuaNImpl.DEFAULT),
			   LuaNEngine::newEnvironment,
			   THREAD_SENSITIVE, ELF32, ELF64)
		);

	public static class Accessor {
		final Runnable INSTALLER;
		final EnvironmentFactory GETTER;
		final CompileTarget[] TARGETS;
		final ThreadPolicy THREAD_POLICY;

		Accessor(Runnable installer, EnvironmentFactory getter,
		         ThreadPolicy threadPolicy, CompileTarget[] targets) {
			this.INSTALLER = installer;
			this.GETTER = getter;
			this.THREAD_POLICY = threadPolicy;
			this.TARGETS = targets;
		}
	}

	public static class Engine {
		final String NAME;
		final Accessor ACCESSOR;

		Engine(String name, Accessor accessor) {
			this.NAME = name;
			this.ACCESSOR = accessor;
		}
	}

	public enum Policy {
		PREFER_NATIVE, PREFER_JAVA
	}

	public enum ThreadPolicy {
		THREAD_SENSITIVE, MULTITHREADED
	}

	@FunctionalInterface
	public interface EnvironmentFactory {
		ScriptGlobals newEnvironment(FuncPool pool, BooleanSupplier terminated,
		                             InputStream stdin, OutputStream stdout, long heap);
	}

	public static Engine engine(String name, Runnable installer, EnvironmentFactory getter,
	                            ThreadPolicy threadPolicy, CompileTarget... targets) {
		Accessor accessor = new Accessor(installer, getter, threadPolicy, targets);
		return new Engine(name, accessor);
	}

	private final HashMap<String, Accessor> MAP = new LinkedHashMap<>();

	public final String FILE_EXTENSION;
	public final Policy POLICY;
	public final String PROCESSOR_PREFIX;

	JointType(String fileExtension, String processorPrefix, Policy policy, Engine... engines) {
		for (Engine engine : engines)
			MAP.put(engine.NAME, engine.ACCESSOR);
		this.FILE_EXTENSION = fileExtension;
		this.POLICY = policy;
		this.PROCESSOR_PREFIX = processorPrefix;
	}

	public Accessor getAccessor(String name) {
		return MAP.get(name);
	}

	public Accessor getDefaultAccessor() {
		switch (POLICY) {
			case PREFER_JAVA:
				for (Accessor accessor : MAP.values())
					if (Arrays.asList(accessor.TARGETS).contains(JAVA))
						return accessor;
				return MAP.values().iterator().next();
			case PREFER_NATIVE:
				for (Accessor accessor : MAP.values())
					if (NativeLoader.canLoadTarget(false, accessor.TARGETS))
						return accessor;
				return MAP.values().iterator().next();
			default: throw new IllegalStateException();
		}
	}

	public Accessor resolveAccessor(Reader reader) {
		try {
			Scanner scanner = new Scanner(reader);
			String ln;
			do ln = scanner.nextLine().trim();
			while (ln.isEmpty());
			if (!ln.startsWith(PROCESSOR_PREFIX))
				return getDefaultAccessor();
			String key = ln.substring(PROCESSOR_PREFIX.length());
			Accessor accessor = MAP.get(key);
			return accessor == null ? getDefaultAccessor() : accessor;
		}
		finally {
			try {
				reader.close();
			}
			catch (Exception ignored) {}
		}
	}

	public static JointType getTypeForExtension(String name) throws EngineBuildException {
		for (JointType set : values())
			if (name.endsWith(set.FILE_EXTENSION))
				return set;
		throw new EngineBuildException("Could not find engine entry for filename: " + name);
	}
}
