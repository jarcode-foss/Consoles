package jarcode.classloading.loader;

import jarcode.controller.base.AbstractGame;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

public final class WrappedClassLoader extends ClassLoader {

	// These are UNLOADED classes, which are removed as soon as they are loaded to conserve memory
	private Map<String, byte[]> classes = new HashMap<>();
	// Loaded classes by this class loader
	private Map<String, Class> loaded = new HashMap<>();

	// Result plugin
	public WrappedPlugin plugin;

	private WrappedPluginLoader loader;
	private Server server;
	private PluginDescriptionFile description;
	private File dataFolder;
	private File file;
	private volatile boolean loadingAll = false;

	private final ClassModifier[] modifiers;

	@SuppressWarnings("unchecked")
	public WrappedClassLoader(InputStream in, WrappedPluginLoader loader, PluginDescriptionFile description,
	                          File dataFolder, File file, ClassModifier... modifiers)
			throws InvalidPluginException, MalformedURLException {
		super(WrappedClassLoader.class.getClassLoader());
		this.loader = loader;
		this.server = loader.server;
		this.description = description;
		this.dataFolder = dataFolder;
		this.modifiers = modifiers;
		this.file = file;
		try {
			JarInputStream inputStream = new JarInputStream(in);
			ZipEntry entry;
			String entryName;
			while ((entry = inputStream.getNextEntry()) != null) {

				entryName = entry.getName();

				if (entryName.endsWith(".class")) {

					byte[] classBytes = IOUtils.toByteArray(inputStream);
					inputStream.closeEntry();
					String transformedName = className(entryName);
					classes.put(transformedName, classBytes);

				}
			}
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Class<? extends WrappedPlugin> pluginClass;
			Class jarClass;
			try {
				jarClass = findClass(description.getMain());
			}
			catch (ClassNotFoundException ex) {
				throw new InvalidPluginException("Cannot find main class '" + description.getMain() + "'", ex);
			}
			try {
				pluginClass = jarClass.asSubclass(WrappedPlugin.class);
			}
			catch (ClassCastException ex) {
				throw new InvalidPluginException("main class '" + description.getMain() + "' does not extend WrappedPlugin", ex);
			}
			try {
				this.plugin = pluginClass.getConstructor().newInstance();
			}
			catch (InvocationTargetException | NoSuchMethodException e) {
				throw new InvalidPluginException("Constructor injection failed", e);
			} catch (IllegalAccessException e) {
				throw new InvalidPluginException("Failed to call constructor", e);
			}
		}
		catch (InstantiationException ex) {
			throw new InvalidPluginException("Abnormal plugin type", ex);
		}
	}

	private String className(String raw) {
		String transformedName = raw.replaceAll("\\\\", ".").replaceAll("/", ".");
		if (transformedName.endsWith(".class"))
			transformedName = transformedName.substring(0, transformedName.length() - 6);
		String split[] = transformedName.split("(==)|(\\$\\.)");
		StringBuilder builder = new StringBuilder();
		builder.append(split[0]);
		for (int t = 1; t < split.length; t++) {
			if (!StringUtils.isNumeric(split[t])) {
				builder.append(".");
				builder.append(split[t]);
			}
			else {
				builder.append("$");
				builder.append(split[t]);
			}
		}
		return builder.toString();
	}

	private byte[] readClass(InputStream stream) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		while(true){
			int i = stream.read();
			if(i == -1) break;
			byteStream.write(i);
		}
		return byteStream.toByteArray();
	}

	public Class loadClass(String name) throws ClassNotFoundException {
		try {
			return this.getParent().loadClass(name);
		} catch (ClassNotFoundException e) {
			return findClass(name);
		}
	}
	public InputStream getResourceAsStream(String str) {
		String name = className(str);
		if (loaded.containsKey(name) || classes.containsKey(name)) {
			return null;
		}
		else return super.getResourceAsStream(str);
	}
	private boolean isAnon(String transformedName) {
		String split[] = transformedName.split("\\$");
		for (int t = 1; t < split.length; t++) {
			if (StringUtils.isNumeric(split[t])) {
				return true;
			}
		}
		return false;
	}

	private boolean hasAnonClasses(String transformedName) {
		for (String name : classes.keySet()) {
			if (name.startsWith(transformedName)
					&& isAnon(name.substring(transformedName.length(), name.length()))) {
				return true;
			}
		}
		return false;
	}
	@Override
	public Class findClass(String name) throws ClassNotFoundException{
		return findClass(name, true);
	}
	private Class findClass(String name, boolean rm) throws ClassNotFoundException {
		try {
			if (loaded.containsKey(name)) {
				return loaded.get(name);
			}
			byte[] classBytes = classes.get(name);
			Class type;

			for (ClassModifier modifier : modifiers)
				classBytes = modifier.instrument(classBytes, name);

			type = defineClass(name, classBytes, 0, classBytes.length);

			loaded.put(name, type);
			loader.setClass(name, type);
			if (rm && !loadingAll)
				classes.remove(name);
			return type;
		}
		catch (NullPointerException e) {
			throw new NullPointerException("Null pointer while trying to access class: " + name);
		}
	}
	public void loadAllClasses() {
		loadingAll = true;
		for (String key : classes.keySet()) {
			try {
				findClass(key, false);
			}
			catch (ClassNotFoundException e) {
				server.getLogger().warning("Failed to load class: " + key);
				e.printStackTrace();
			}
		}
		classes.clear();
	}
	public void unload() {
		classes.clear();
		loaded.clear();
	}
	void initialize(WrappedPlugin plugin) {
		Validate.notNull(plugin, "Initializing plugin cannot be null");
		Validate.isTrue(plugin.getClass().getClassLoader() == this, "Cannot initialize plugin outside of this class loader");

		plugin.init(this.loader, this.loader.server, this.description, this.dataFolder, this.file, this);
	}

	public List<String> getClassPaths() {
		ArrayList<String> list = new ArrayList<>();
		list.addAll(loaded.keySet());
		list.addAll(classes.keySet());
		return list;
	}
	public Set<String> getLoadedClasses() {
		return loaded.keySet();
	}
}
