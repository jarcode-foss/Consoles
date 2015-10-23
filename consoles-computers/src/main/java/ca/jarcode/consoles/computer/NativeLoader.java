package ca.jarcode.consoles.computer;

import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;

public class NativeLoader {

	public static final Function<String, String> libraryFormatter = libraryFormat();

	public interface ILoader {
		long dlopen(String path);
		int dlclose(long handle);
	}

	private static ILoader loader;

	private static Function<String, String> libraryFormat() {
		String os = System.getProperty("os.name");
		boolean arch32 = System.getProperty("os.arch").contains("86"); // check for x86 arch, otherwise assume amd64
		if (os.contains("Linux") || os.contains("Android"))
			return (lib) -> "lib" + lib.toLowerCase() + (arch32 ? "32" : "") + ".so";
		else if (os.contains("Windows"))
			return (lib) -> lib + (arch32 ? "32" : "") + ".dll";
		else if (os.contains("Mac"))
			return (lib) -> "lib" + lib.toLowerCase() + (arch32 ? "32" : "") + ".dylib";
		else return null;
	}

	private static String libraryExtension() {
		String os = System.getProperty("os.name");
		if (os.contains("Linux") || os.contains("Android"))
			return "so";
		else if (os.contains("Windows"))
			return "dll";
		else if (os.contains("Mac"))
			return "dylib";
		else return null;
	}

	public static void linkLoader(ILoader loader) {
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
	public void loadAsJNILibrary(Plugin plugin) {
		File file = extractLib(plugin);
		System.load(file.getAbsolutePath());
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
				throw new RuntimeException("Unsupported platform");
			File folder = plugin.getDataFolder();
			result = new File(folder, lib);
			if (result.exists())
				result.delete();
			Files.copy(stream, Paths.get(result.getAbsolutePath()));
			return result;
		} catch (FileNotFoundException e) {
			plugin.getLogger().severe("failed to find target library file");
			throw new RuntimeException("Unsupported platform");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Unsupported platform");
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
