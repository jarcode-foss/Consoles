package ca.jarcode.consoles.computer;

import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;

public class NativeLoader {

	public static final Function<String, String> libraryFormatter = libraryFormat(getNativeTarget());

	public interface ILoader {
		long dlopen(String path);
		int dlclose(long handle);
	}

	private static ILoader loader;

	public static Function<String, String> libraryFormat(CompileTarget target) {
		switch (target) {
			case ELF32: return (lib) -> "lib" + lib + "32.so";
			case ELF64: return (lib) -> "lib" + lib + ".so";
			case WIN32: return (lib) -> lib + "32.dll";
			case WIN64: return (lib) -> lib + ".dll";
			case OSX: return (lib) -> lib + "dylib";
			default: throw new RuntimeException("Unsupported platform");
		}
	}

	public static boolean arch32() {
		return System.getProperty("os.arch").contains("86") && !System.getProperty("os.arch").contains("x64_86");
	}

	public static CompileTarget getNativeTarget() {
		String os = System.getProperty("os.name");
		boolean arch32 = arch32();
		if (os.contains("Linux") || os.contains("FreeBSD") || os.contains("Android"))
			return (arch32 ? CompileTarget.ELF32 : CompileTarget.ELF64);
		else if (os.contains("Windows"))
			return (arch32 ? CompileTarget.WIN32 : CompileTarget.WIN64);
		else if (os.contains("Mac"))
			return CompileTarget.OSX;
		else throw new RuntimeException("Unknown platform");
	};

	private static String libraryExtension() {
		CompileTarget target = getNativeTarget();
		switch (target) {
			case ELF32:
			case ELF64: return "so";
			case WIN32:
			case WIN64: return "dll";
			case OSX: return "dylib";
			default: throw new RuntimeException("Unsupported platform");
		}
	}

	public static void linkLoader(ILoader loader) {
		if (loader != null)
			NativeLoader.loader = loader;
	}

	public static int closeDynamicLibrary(long handle) {
		if (loader == null)
			throw new UnsupportedOperationException();
		else return loader.dlclose(handle);
	}

	private final String libraryName;

	public NativeLoader(String name) {
		libraryName = name;
	}

	/**
	 * Loads the library as a JNI library, implementing native methods
	 *
	 * @param plugin parent plugin
	 */
	public boolean loadAsJNILibrary(Plugin plugin) {
		File file = extractLib(plugin);
		if (file == null) return false;
		try {
			System.load(file.getAbsolutePath());
		}
		catch (Throwable e) {
			plugin.getLogger().severe("failed to load library (do have have the right dependencies installed?)");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Loads the library as a dynamic library, returning a handle
	 *
	 * @param plugin parent plugin
	 * @return a pointer to the library handle (OS specific), can be 32 bit or 64 bit pointer.
	 */
	public long loadAsDynamicLibrary(Plugin plugin) {
		if (loader == null)
			throw new UnsupportedOperationException();
		else return loader.dlopen(plugin.getDataFolder().getAbsolutePath()
				+ File.separator + libraryFormatter.apply(libraryName));
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public File extractLib(Plugin plugin) {
		String lib = libraryFormatter.apply(libraryName);
		InputStream stream = null;
		File result;
		try {
			stream = this.getClass()
					.getClassLoader().getResourceAsStream(lib);
			if (stream == null)
				return null;
			File folder = plugin.getDataFolder();
			if (!folder.exists())
				if (!folder.mkdir())
					return null;
			result = new File(folder, lib);
			if (result.exists())
				result.delete();
			Files.copy(stream, Paths.get(result.getAbsolutePath()));
			return result;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			plugin.getLogger().severe("failed to extract library" + lib);
			e.printStackTrace();
			return null;
		}
		finally {
			silentlyClose(stream);
		}
	}
	@SuppressWarnings("EmptyCatchBlock")
	private void silentlyClose(Closeable... closeables) {
		for (Closeable closeable : closeables)
			if (closeable != null) try {
				closeable.close();
			}
			catch (Throwable e) {}
	}
}
