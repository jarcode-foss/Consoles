package jarcode.consoles;

import jarcode.classloading.loader.MinecraftVersionModifier;
import jarcode.classloading.loader.WrappedPluginLoader;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

// yes, this is the main plugin class. It actually causes the plugin to load itself,
// the reason being so I can interrupt class loading myself.
// yes, I'm evil. Very evil.
public class ConsolesLoader extends JavaPlugin {
	{
		WrappedPluginLoader loader = WrappedPluginLoader.inject(this, new MinecraftVersionModifier());
		try {
			Plugin plugin = loader.loadPlugin(this.getFile());
			loader.enablePlugin(plugin);
		} catch (InvalidPluginException e) {
			getLogger().severe("Failed to load underlying Consoles plugin!");
			e.printStackTrace();
		}
	}
}
