package jarcode.consoles.computer.interpreter;

import jarcode.classloading.loader.WrappedPlugin;
import jarcode.consoles.Consoles;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.boot.Kernel;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFolder;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LuaDefaults {
	public static final HashMap<String, String> SCRIPTS = new HashMap<>();

	static {
		SCRIPTS.clear();
		try {
			File jar = new File(WrappedPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			ZipFile file = new ZipFile(jar);
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.getName().startsWith("lua/") && entry.getName().endsWith(".lua")) {
					InputStream stream = file.getInputStream(entry);
					String content = IOUtils.readLines(stream).stream().collect(Collectors.joining("\n"));
					stream.close();
					String formatted = entry.getName().substring(4, entry.getName().length() - 4);
					if (Consoles.DEBUG)
						Consoles.getInstance().getLogger().info("[DEBUG] Loaded: " + formatted);
					SCRIPTS.put(formatted, content);
				}
			}
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}
	}
	public static void loadInto(Computer computer) {
		for (Map.Entry<String, String> entry : SCRIPTS.entrySet()) {
			String[] arr = entry.getKey().split("/");
			String dir = Arrays.asList(arr).stream()
					.limit(arr.length <= 1 ? 1 : arr.length - 1)
					.collect(Collectors.joining("/"));
			String f = Arrays.asList(arr).stream()
					.skip(arr.length <= 1 ? 0 : arr.length - 1)
					.findFirst().orElseGet(() -> null);
			assert f != null;
			boolean result = computer.getRoot().mkdir(dir);
			if (!result)
				Consoles.getInstance().getLogger().info("Failed to install: " + entry.getKey());
			else {
				try {
					FSFolder folder = (FSFolder) computer.getRoot().get(dir);
					folder.contents.put(f, Kernel.writtenFile(entry.getValue()));
				} catch (FileNotFoundException e) {
					Consoles.getInstance().getLogger().info("Failed to install: " + entry.getKey());
					e.printStackTrace();
				}
			}
		}
	}
}
