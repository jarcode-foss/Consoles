package ca.jarcode.consoles.computer.interpreter;

import ca.jarcode.consoles.Computers;
import ca.jarcode.consoles.Consoles;
import ca.jarcode.consoles.computer.Computer;
import ca.jarcode.consoles.computer.boot.Kernel;
import ca.jarcode.consoles.computer.filesystem.FSFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*

This is for loading from the /lua resource folder.

 */
public class LuaDefaults {

	public static final HashMap<String, String> SCRIPTS = new HashMap<>();
	public static final HashMap<String, String> TESTS = new HashMap<>();

	static {
		SCRIPTS.clear();
		loadPackagedDir("provided", SCRIPTS::put);
	}

	public static void loadPackagedDir(String dir, BiConsumer<String, String> consumer) {
		try {
			File jar = Computers.jarFile;
			ZipFile file = new ZipFile(jar);
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.getName().startsWith(dir + "/") && entry.getName().endsWith(".lua")) {
					InputStream stream = file.getInputStream(entry);
					int size = stream.available();
					int read = 0;
					byte[] buf = new byte[size];
					while (read < size)
						read = stream.read(buf, read, size - read);
					String content = new String(buf, StandardCharsets.UTF_8);
					stream.close();
					String formatted = entry.getName().substring(4, entry.getName().length() - 4);
					if (Computers.debug)
						Computers.getInstance().getLogger().info("[DEBUG] Loaded: " + formatted);
					consumer.accept(formatted, content);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void cacheTests() {
		TESTS.clear();
		loadPackagedDir("tests", TESTS::put);
	}

	public static void loadInto(Computer computer, HashMap<String, String> mapped, String prefix) {
		for (Map.Entry<String, String> entry : mapped.entrySet()) {
			String fullEntry = (prefix == null ? "" : prefix) + entry.getKey();
			String[] arr = fullEntry.split("/");
			String dir = Arrays.asList(arr).stream()
					.limit(arr.length <= 1 ? 1 : arr.length - 1)
					.collect(Collectors.joining("/"));
			String f = Arrays.asList(arr).stream()
					.skip(arr.length <= 1 ? 0 : arr.length - 1)
					.findFirst().orElseGet(() -> null);
			assert f != null;
			boolean result = computer.getRoot().mkdir(dir);
			if (result) {
				try {
					FSFolder folder = (FSFolder) computer.getRoot().get(dir);
					folder.contents.put(f, Kernel.writtenFile(entry.getValue(), computer));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void loadProvidedScripts(Computer computer) {
		// load all provided scripts into /
		loadInto(computer, SCRIPTS, null);
	}

	public static void loadTests(Computer computer) {
		// load tests into /bin/tests/...
		loadInto(computer, TESTS, "bin/");
	}
}
