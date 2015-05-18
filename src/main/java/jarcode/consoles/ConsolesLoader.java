package jarcode.consoles;

import jarcode.classloading.loader.MinecraftVersionModifier;
import jarcode.classloading.loader.WrappedPluginLoader;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/*

Alright, Consoles uses a plugin loader - which means the plugin actually loads itself. The reason
why we do this is to actually do some hacks during class loading to attempt at cross-version compatibility.

So, package versions are instantly corrected at runtime, but this doesn't mean everything will work in other
versions, it just means we're going to try to get it to work.

When we need to, we can switch on Pkg.VERSION to check the package naming of the server, thus allowing us
to write code specific for each version.

The goal of this system to to provide support for server versions 1.8.3 and higher, and fields and methods
that have multiple names across versions will be replaced with reflection over time.

 */
@SuppressWarnings("unchecked")
public class ConsolesLoader extends JavaPlugin {

	// This is the version we compile against, which should be the latest spigot/CB build.
	private static final String COMPILED_VERSION = "v1_8_R3";

	private static Object grab(Class from, Object inst, String name)
			throws IllegalAccessException, NoSuchFieldException {
		Field field = from.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(inst);
	}

	private Runnable enableTask;

	{
		// inject our plugin loader
		WrappedPluginLoader loader = WrappedPluginLoader.inject(this,
				new MinecraftVersionModifier(this, COMPILED_VERSION));
		try {
			// load plugin through wrapper loader (direct loading)
			Plugin plugin = loader.loadPlugin(this.getFile());

			// we'll use this to enable our plugin later
			enableTask = () -> loader.enablePlugin(plugin);

			// we still need to properly register the plugin in the plugin manager,
			// so we use some reflection hacks to do so.
			PluginManager manager = getServer().getPluginManager();
			// grab the lookup name map and plugin list
			Map<String, Plugin> map = (Map<String, Plugin>) grab(SimplePluginManager.class, manager, "lookupNames");
			List<Plugin> plugins = (List<Plugin>) grab(SimplePluginManager.class, manager, "plugins");
			// add our plugin to the manager!
			map.put(plugin.getDescription().getName(), plugin);
			plugins.add(plugin);
		} catch (Throwable e) {
			getLogger().severe("Failed to load underlying Consoles plugin!");
			e.printStackTrace();
		}
	}

	@Override
	public void onEnable() {
		enableTask.run();

		// now this plugin is relatively useless to keep running, so we're going
		// to disable and unload it.

		// disable our plugin normally
		getServer().getPluginManager().disablePlugin(this);

		PluginManager manager = getServer().getPluginManager();

		try {
			// grab the lookup name map and plugin list, just like before
			Map<String, Plugin> map = (Map<String, Plugin>) grab(SimplePluginManager.class, manager, "lookupNames");
			List<Plugin> plugins = (List<Plugin>) grab(SimplePluginManager.class, manager, "plugins");

			// remove this plugin from the mappings and the plugin list
			map.remove(getDescription().getName());
			plugins.remove(this);
		} catch (Throwable e) {
			getLogger().severe("Failed to unload Consoles plugin loader!");
			e.printStackTrace();
		}
	}
}
