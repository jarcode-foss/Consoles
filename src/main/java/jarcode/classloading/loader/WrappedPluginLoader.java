package jarcode.classloading.loader;

import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class WrappedPluginLoader implements PluginLoader {

	private static final Field ASSOCIATIONS;

	static {
		Field f1 = null;
		try {
			f1 = SimplePluginManager.class.getDeclaredField("fileAssociations");
			f1.setAccessible(true);
		}
		catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		ASSOCIATIONS = f1;
	}

	@SuppressWarnings("unchecked")
	private static Map<Pattern, PluginLoader> getAssociations(Object instance) {
		try {
			return (Map<Pattern, PluginLoader>) ASSOCIATIONS.get(instance);
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static WrappedPluginLoader inject(Plugin source, ClassModifier... modifiers) {

		final Server server = source.getServer();

		Map<Pattern, PluginLoader> map = getAssociations(server.getPluginManager());
		// If there's a loader already injected, don't inject any more!
		for (PluginLoader loader : map.values()) {
			if (loader instanceof WrappedPluginLoader) {
				return (WrappedPluginLoader) loader;
			}
		}
		WrappedPluginLoader loader = new WrappedPluginLoader(server, modifiers);

		// Inject the loader with its file filters
		for (Pattern pattern : loader.getPluginFileFilters()) {
			map.put(pattern, loader);
		}

		return loader;
	}

	public static boolean isJar(byte[] data) {
		return data != null && data[0] == 0x50 && data[1] == 0x4B && data[2] == 0x03 && data[3] == 0x04;
	}

	private final Map<String, WrappedClassLoader> loaders = new LinkedHashMap<>();
	private final Map<String, PluginDescriptionFile> descriptionFiles = new HashMap<>();

	final Server server;
	// regex that doesn't match anything
	private final Pattern[] fileFilters = { Pattern.compile("a^") };
	private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

	private final ClassModifier[] modifiers;

	// This is used to steal methods from the default plugin loader
	private final JavaPluginLoader javaPluginLoader;
	@SuppressWarnings("deprecation")
	public WrappedPluginLoader(Server instance, ClassModifier... modifiers) {
		Validate.notNull(instance, "Server cannot be null");
		this.server = instance;
		this.javaPluginLoader = new JavaPluginLoader(instance);
		this.modifiers = modifiers;
	}

	public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
		return descriptionFiles.get(file.getAbsolutePath());
	}
	private boolean getDescription(File file) throws InvalidDescriptionException {
		JarInputStream jar = null;
		PluginDescriptionFile desc = null;
		try {
			byte[] array = Files.readAllBytes(Paths.get(file.getAbsolutePath()));

			if (!isJar(array)) return false;
			jar = new JarInputStream(new ByteArrayInputStream(array));
			JarEntry entry;
			while((entry = jar.getNextJarEntry()) != null) {
				if (entry.getName().equals("plugin_wrapped.yml")) {
					desc = new PluginDescriptionFile(jar);
				}
			}
		}
		catch (IOException e) {
			throw new InvalidDescriptionException("Could not load wrapped jar file");
		}
		finally {
			if (jar != null) {
				try {
					jar.close();
				}
				catch (IOException ignore) {}
			}
		}
		if (desc != null)
			descriptionFiles.put(file.getAbsolutePath(), desc);
		else throw new InvalidDescriptionException("No plugin.yml found!");
		return true;
	}
	public void disablePlugin(Plugin plugin) {
		disablePlugin(plugin, true);
	}
	@SuppressWarnings("ConstantConditions")
	public void disablePlugin(Plugin plugin, boolean call) {
		Validate.isTrue(plugin instanceof WrappedPlugin, "Plugin is not associated with this PluginLoader");
		if (plugin.isEnabled()) {
			plugin.getLogger().info(String.format("Disabling %s", plugin.getDescription().getFullName()));
			if (call)
				this.server.getPluginManager().callEvent(new PluginDisableEvent(plugin));
			if (call)
				((WrappedPlugin) plugin).setEnabled(false);
			else
				((WrappedPlugin) plugin).setEnabled(false, false);
			WrappedClassLoader loader = loaders.remove(plugin.getDescription().getName());
			Set<String> names = loader.getLoadedClasses();

			names.forEach(this::removeClass);

			// nullify class loader reference so it can be collected easier
			((WrappedPlugin) plugin).classLoader = null;

			// unload StreamClassLoader's class cache
			loader.unload();
		}
	}
	@SuppressWarnings("unchecked")
	public void setClass(String name, Class<?> clazz) {
		if (!this.classes.containsKey(name)) {
			this.classes.put(name, clazz);

			if (ConfigurationSerializable.class.isAssignableFrom(clazz)) {
				Class serializable = clazz.asSubclass(ConfigurationSerializable.class);
				ConfigurationSerialization.registerClass(serializable);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void removeClass(String name) {
		Class clazz = (Class)this.classes.remove(name);
		try
		{
			if ((clazz != null) && (ConfigurationSerializable.class.isAssignableFrom(clazz))) {
				Class serializable = clazz.asSubclass(ConfigurationSerializable.class);
				ConfigurationSerialization.unregisterClass(serializable);
			}
		} catch (NullPointerException ignored) {}
	}
	@SuppressWarnings("ConstantConditions")
	public void enablePlugin(Plugin plugin) {
		Validate.isTrue(plugin instanceof WrappedPlugin, "Plugin is not associated with this PluginLoader");
		if (!plugin.isEnabled()) {
			plugin.getLogger().info("Enabling " + plugin.getDescription().getFullName());
			String pluginName = plugin.getName();
			try {
				if (!this.loaders.containsKey(pluginName)) {
					ClassLoader loader = ((WrappedPlugin) plugin).classLoader;
					Validate.isTrue(loader instanceof WrappedClassLoader, "Plugin class loader is of a different type: " + loader.getClass().getCanonicalName());
					if (loader instanceof WrappedClassLoader)
						this.loaders.put(pluginName, (WrappedClassLoader) loader);
				}
			}
			catch (Throwable e) {
				this.server.getLogger().log(Level.SEVERE, "Failed to obtain class loader from " + plugin.getDescription().getFullName(), e);
				return;
			}
			((WrappedPlugin) plugin).setEnabled(true);

			this.server.getPluginManager().callEvent(new PluginEnableEvent(plugin));
		}
	}
	public Map<Class<? extends org.bukkit.event.Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
	return javaPluginLoader.createRegisteredListeners(listener, plugin);
	}
	public Pattern[] getPluginFileFilters() {
		return fileFilters;
	}
	@Override
	public Plugin loadPlugin(File file) throws InvalidPluginException {
		Validate.notNull(file, "File cannot be null");

		if (!file.exists()) {
			throw new InvalidPluginException(new FileNotFoundException(file.getPath() + " does not exist"));
		}

		PluginDescriptionFile description;

		try {
			boolean result = getDescription(file);
			if (!result)
				throw new InvalidPluginException("Bad jar file");
			description = getPluginDescription(file);
		}
		catch (InvalidDescriptionException ex) {
			throw new InvalidPluginException(ex);
		}

		File parentFile = file.getParentFile();
		File dataFolder = new File(parentFile, description.getName());

		@SuppressWarnings("deprecation")
		File oldDataFolder = new File(parentFile, description.getRawName());

		if (!dataFolder.equals(oldDataFolder))
		{
			if ((dataFolder.isDirectory()) && (oldDataFolder.isDirectory())) {
				this.server.getLogger().warning(String.format(
						"While loading %s (%s) found old-data folder: '%s' next to the new one '%s'", description.getFullName(),
						file,
						oldDataFolder,
						dataFolder));
			}
			else if ((oldDataFolder.isDirectory()) && (!dataFolder.exists())) {
				if (!oldDataFolder.renameTo(dataFolder)) {
					throw new InvalidPluginException("Unable to rename old data folder: '" + oldDataFolder + "' to: '" + dataFolder + "'");
				}
				this.server.getLogger().log(Level.INFO, String.format(
						"While loading %s (%s) renamed data folder: '%s' to '%s'", description.getFullName(),
						file,
						oldDataFolder,
						dataFolder));
			}
		}

		if ((dataFolder.exists()) && (!dataFolder.isDirectory())) {
			throw new InvalidPluginException(String.format(
					"Projected datafolder: '%s' for %s (%s) exists and is not a directory", dataFolder,
							description.getFullName(),
							file));
		}
		byte[] array;
		try {
			array = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
		}
		catch (Throwable e) {
			throw new InvalidPluginException(e);
		}

		ByteArrayInputStream is = new ByteArrayInputStream(array);

		WrappedClassLoader loader;
		try {
			loader = new WrappedClassLoader(is, this, description, dataFolder, file, modifiers);
		}
		catch (InvalidPluginException ex) {
			throw ex;
		} catch (Throwable ex) {
			throw new InvalidPluginException(ex);
		}
		this.loaders.put(description.getName(), loader);

		return loader.plugin;
	}
}
