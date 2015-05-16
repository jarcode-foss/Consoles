package jarcode.classloading.loader;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.Warning;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*

Plugin implementation that we use for Consoles. Most of this
code is the same as a normal JavaPlugin.

 */
public abstract class WrappedPlugin extends PluginBase {
	private boolean isEnabled = false;
	private PluginLoader loader = null;
	private Server server = null;
	private File file = null;
	private PluginDescriptionFile description = null;
	private File dataFolder = null;
	ClassLoader classLoader = null;
	private boolean naggable = true;
	private EbeanServer ebean = null;
	private FileConfiguration newConfig = null;
	private File configFile = null;
	private PluginLogger logger = null;
	private boolean loadedAll = false;

	public WrappedPlugin() {
		ClassLoader classLoader = getClass().getClassLoader();
		if (!(classLoader instanceof WrappedClassLoader)) {
			throw new IllegalStateException("WrappedPlugin requires " + WrappedClassLoader.class.getName());
		}
		((WrappedClassLoader) classLoader).initialize(this);
	}

	public final File getDataFolder()
	{
		return this.dataFolder;
	}

	public final PluginLoader getPluginLoader()
	{
		return this.loader;
	}

	public final Server getServer()
	{
		return this.server;
	}

	public final boolean isEnabled()
	{
		return this.isEnabled;
	}

	protected File getFile()
	{
		return this.file;
	}

	public final PluginDescriptionFile getDescription()
	{
		return this.description;
	}

	public FileConfiguration getConfig() {
		if (this.newConfig == null) {
			reloadConfig();
		}
		return this.newConfig;
	}

	public void loadAllClasses() {
		if (!loadedAll) {
			((WrappedClassLoader) getClassLoader()).loadAllClasses();
			loadedAll = true;
		}
	}

	protected final Reader getTextResource(String file)
	{
		InputStream in = getResource(file);

		return in == null ? null : new InputStreamReader(in, (isStrictlyUTF8()) || (FileConfiguration.UTF8_OVERRIDE) ? Charsets.UTF_8 : Charset.defaultCharset());
	}

	public void reloadConfig()
	{
		this.newConfig = YamlConfiguration.loadConfiguration(this.configFile);

		InputStream defConfigStream = getResource("config.yml");
		if (defConfigStream == null)
			return;
		YamlConfiguration defConfig;
		if ((isStrictlyUTF8()) || (FileConfiguration.UTF8_OVERRIDE)) {
			defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8));
		}
		else {
			defConfig = new YamlConfiguration();
			byte[] contents;
			try {
				contents = ByteStreams.toByteArray(defConfigStream);
			}
			catch (IOException e)
			{
				getLogger().log(Level.SEVERE, "Unexpected failure reading config.yml", e);
				return;
			}
			String text = new String(contents, Charset.defaultCharset());
			if (!text.equals(new String(contents, Charsets.UTF_8))) {
				getLogger().warning("Default system encoding may have misread config.yml from plugin jar");
			}
			try
			{
				defConfig.loadFromString(text);
			} catch (InvalidConfigurationException e) {
				getLogger().log(Level.SEVERE, "Cannot load configuration from jar", e);
			}
		}

		this.newConfig.setDefaults(defConfig);
	}

	private boolean isStrictlyUTF8() {
		return getDescription().getAwareness().contains(PluginAwareness.Flags.UTF8);
	}

	public void saveConfig()
	{
		try {
			getConfig().save(this.configFile);
		} catch (IOException ex) {
			this.logger.log(Level.SEVERE, "Could not save config to " + this.configFile, ex);
		}
	}

	public void saveDefaultConfig()
	{
		if (!this.configFile.exists())
			saveResource("config.yml", false);
	}

	public void saveResource(String resourcePath, boolean replace)
	{
		if ((resourcePath == null) || (resourcePath.equals(""))) {
			throw new IllegalArgumentException("ResourcePath cannot be null or empty");
		}

		resourcePath = resourcePath.replace('\\', '/');
		InputStream in = getResource(resourcePath);
		if (in == null) {
			throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + this.file);
		}

		File outFile = new File(this.dataFolder, resourcePath);
		int lastIndex = resourcePath.lastIndexOf('/');
		File outDir = new File(this.dataFolder, resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

		if (!outDir.exists()) {
			//noinspection ResultOfMethodCallIgnored
			outDir.mkdirs();
		}
		try
		{
			if ((!outFile.exists()) || (replace)) {
				OutputStream out = new FileOutputStream(outFile);
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0)
				{
					out.write(buf, 0, len);
				}
				out.close();
				in.close();
			} else {
				this.logger.log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
			}
		} catch (IOException ex) {
			this.logger.log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
		}
	}


	public InputStream getResource(String filename)
	{
		if (filename == null) {
			throw new IllegalArgumentException("Filename cannot be null");
		}
		ZipFile file = null;
		try {
			file = new ZipFile(getFile());
			Enumeration<? extends ZipEntry> e = file.entries();
			while (e.hasMoreElements()) {
				ZipEntry entry = e.nextElement();
				if (entry.getName().equals(filename)) {
					InputStream is = file.getInputStream(entry);
					byte[] data = new byte[is.available()];
					IOUtils.readFully(file.getInputStream(entry), data);
					return new ByteArrayInputStream(data);
				}
			}
		} catch (IOException e) {
			getLogger().severe("Failed to load resource");
			e.printStackTrace();
		}
		finally {
			if (file != null)
				try {
					file.close();
				} catch (IOException ignored) {}
		}
		return null;
	}

	protected final ClassLoader getClassLoader()
	{
		return this.classLoader;
	}

	protected final void setEnabled(boolean enabled) {
		setEnabled(enabled, true);
	}

	protected final void setEnabled(boolean enabled, boolean call)
	{
		if (this.isEnabled != enabled) {
			this.isEnabled = enabled;
			if (call) {
				if (this.isEnabled)
					onEnable();
				else
					onDisable();
			}
		}
	}

	@Deprecated
	protected final void initialize(PluginLoader loader, Server server, PluginDescriptionFile description, File dataFolder, File file, ClassLoader classLoader)
	{
		if (server.getWarningState() == Warning.WarningState.OFF) {
			return;
		}
		getLogger().log(Level.WARNING, getClass().getName() + " is already initialized", server.getWarningState() == Warning.WarningState.DEFAULT ? null : new AuthorNagException("Explicit initialization"));
	}

	final void init(PluginLoader loader, Server server, PluginDescriptionFile description, File dataFolder, File file, ClassLoader classLoader) {
		this.loader = loader;
		this.server = server;
		this.file = file;
		this.description = description;
		this.dataFolder = dataFolder;
		this.classLoader = classLoader;
		this.configFile = new File(dataFolder, "config.yml");
		this.logger = new PluginLogger(this);

		if (description.isDatabaseEnabled()) {
			ServerConfig db = new ServerConfig();

			db.setDefaultServer(false);
			db.setRegister(false);
			db.setClasses(getDatabaseClasses());
			db.setName(description.getName());
			server.configureDbConfig(db);

			DataSourceConfig ds = db.getDataSourceConfig();

			ds.setUrl(replaceDatabaseString(ds.getUrl()));
			dataFolder.mkdirs();

			ClassLoader previous = Thread.currentThread().getContextClassLoader();

			Thread.currentThread().setContextClassLoader(classLoader);
			this.ebean = EbeanServerFactory.create(db);
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	public List<Class<?>> getDatabaseClasses()
	{
		return new ArrayList<>();
	}

	private String replaceDatabaseString(String input) {
		input = input.replaceAll("\\{DIR\\}", this.dataFolder.getPath().replaceAll("\\\\", "/") + "/");
		input = input.replaceAll("\\{NAME\\}", this.description.getName().replaceAll("[^\\w_-]", ""));
		return input;
	}

	@Deprecated
	public final boolean isInitialized()
	{
		return true;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		return false;
	}

	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
	{
		return null;
	}

	public PluginCommand getCommand(String name)
	{
		String alias = name.toLowerCase();
		PluginCommand command = getServer().getPluginCommand(alias);

		if ((command == null) || (command.getPlugin() != this)) {
			command = getServer().getPluginCommand(this.description.getName().toLowerCase() + ":" + alias);
		}

		if ((command != null) && (command.getPlugin() == this)) {
			return command;
		}
		return null;
	}

	public void onLoad()
	{
	}

	public void onDisable()
	{
	}

	public void onEnable() {
	}

	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
		return null;
	}

	public final boolean isNaggable()
	{
		return this.naggable;
	}

	public final void setNaggable(boolean canNag)
	{
		this.naggable = canNag;
	}

	public EbeanServer getDatabase()
	{
		return this.ebean;
	}

	protected void installDDL() {
		SpiEbeanServer serv = (SpiEbeanServer)getDatabase();
		DdlGenerator gen = serv.getDdlGenerator();

		gen.runScript(false, gen.generateCreateDdl());
	}

	protected void removeDDL() {
		SpiEbeanServer serv = (SpiEbeanServer)getDatabase();
		DdlGenerator gen = serv.getDdlGenerator();

		gen.runScript(true, gen.generateDropDdl());
	}

	public final Logger getLogger()
	{
		return this.logger;
	}

	public String toString()
	{
		return this.description.getFullName();
	}

	public static <T extends WrappedPlugin> T getPlugin(Class<T> clazz)
	{
		Validate.notNull(clazz, "Null class cannot have a plugin");
		if (!WrappedPlugin.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException(clazz + " does not extend " + WrappedPlugin.class);
		}
		ClassLoader cl = clazz.getClassLoader();
		if (!(cl instanceof WrappedClassLoader)) {
			throw new IllegalArgumentException(clazz + " is not initialized by " + WrappedClassLoader.class);
		}
		WrappedPlugin plugin = ((WrappedClassLoader)cl).plugin;
		if (plugin == null) {
			throw new IllegalStateException("Cannot get plugin for " + clazz + " from a static initializer");
		}
		return clazz.cast(plugin);
	}

	public static WrappedPlugin getProvidingPlugin(Class<?> clazz) {

		Validate.notNull(clazz, "Null class cannot have a plugin");
		ClassLoader cl = clazz.getClassLoader();
		if (!(cl instanceof WrappedClassLoader)) {
			throw new IllegalArgumentException(clazz + " is not provided by " + WrappedClassLoader.class);
		}
		WrappedPlugin plugin = ((WrappedClassLoader)cl).plugin;
		if (plugin == null) {
			throw new IllegalStateException("Cannot get plugin for " + clazz + " from a static initializer");
		}
		return plugin;
	}
}