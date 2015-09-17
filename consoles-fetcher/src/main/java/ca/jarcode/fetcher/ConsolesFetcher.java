package ca.jarcode.fetcher;

import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * This class contains functionality to grab consoles modules from a maven repository.
 *
 * You may use this class in any piece of code, for any purpose, without any restrictions. You are free to modify and
 * distribute with only the request (not obligation) that you release the source.
 *
 * @author Jarcode
 *
 */
public class ConsolesFetcher {

	private static final String GROUP_ID, ARTIFACT_ID, ARTIFACT_SUFFIX, ARTIFACT_TYPE, REPOSITORY_URL;

	private static final String PLUGIN_NAME = "ConsolesCore";

	private static final Method GET_PLUGIN_FILE;

	static {
		GROUP_ID = "ca.jarcode";
		ARTIFACT_ID = "consoles-core";
		ARTIFACT_SUFFIX = "-jar-with-dependencies";
		ARTIFACT_TYPE = "jar";
		REPOSITORY_URL = "http://jarcode.ca/maven2";

		try {
			GET_PLUGIN_FILE = JavaPlugin.class.getDeclaredMethod("getFile");
			GET_PLUGIN_FILE.setAccessible(true);
		} catch (NoSuchMethodException e) {
			// shouldn't happen, unless something serious with the Bukkit API changes.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Guarantees that the 'consoles-core' plugin is installed. If it exists, this method will do nothing, otherwise
	 * it will download it from a maven repository (with dependencies) and install it.
	 *
	 * @param version the versions of 'consoles-core' to download, if it doesn't exist
	 * @param sourcePlugin the calling plugin
	 * @throws IOException if an issue occurs while downloading
	 * @throws InvalidDescriptionException if the downloaded artifact has an invalid plugin description
	 * @throws InvalidPluginException if the download artifact is not a valid java plugin
	 */
	public static void install(Plugin sourcePlugin, String version)
			throws IOException, InvalidDescriptionException, InvalidPluginException {

		// We use the calling plugin to safely determine the plugin folder which (technically) doesn't have to be 'plugins'
		if (!(sourcePlugin instanceof JavaPlugin))
			throw new IllegalArgumentException("sourcePlugin must be a JavaPlugin!");

		// If the core plugin is already running, just return
		if (Bukkit.getPluginManager().getPlugin(PLUGIN_NAME) != null)
			return;

		InputStream stream = null;
		ReadableByteChannel channel = null;
		FileOutputStream out = null;

		String finalPath = null;

		try {
			String source = REPOSITORY_URL + '/' + GROUP_ID.replace(".", "/") + '/'
					+ ARTIFACT_ID + '/' + version
					+ ARTIFACT_ID + '-' + version + ARTIFACT_SUFFIX + '.' + ARTIFACT_TYPE;

			File callingPluginFile;

			// This reflection trick is being used because JavaPlugin.getFile() isn't visible outside of the class.
			try {
				callingPluginFile = (File) GET_PLUGIN_FILE.invoke(sourcePlugin);
			} catch (IllegalAccessException | InvocationTargetException e) {
				// again, this really shouldn't happen.
				throw new RuntimeException(e);
			}

			finalPath = callingPluginFile.getParentFile().getAbsolutePath() + File.separatorChar
					+ ARTIFACT_ID + '-' + version + '.' + ARTIFACT_TYPE;

			stream = new URL(source).openStream();
			channel = Channels.newChannel(stream);
			out = new FileOutputStream(finalPath);
			out.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
		}
		finally {
			silentlyClose(stream, channel, out);
		}

		Bukkit.getPluginManager().loadPlugin(new File(finalPath));

	}
	@SuppressWarnings("EmptyCatchBlock")
	private static void silentlyClose(Closeable... closeables) {
		for (Closeable closeable : closeables) {
			if (closeable != null) try {
				closeable.close();
			}
			catch (IOException e) {}
		}
	}
}
