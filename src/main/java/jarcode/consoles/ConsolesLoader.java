package jarcode.consoles;

import jarcode.classloading.loader.MinecraftVersionModifier;
import jarcode.classloading.loader.WrappedPluginLoader;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/*

Alright, Consoles uses a plugin loader - which means the plugin actually loads itself. The reason
why we do this is to actually do some hacks during class loading to attempt at cross-version compatibility.

So, package versions are instantly corrected at runtime, but this doesn't mean everything will work in other
versions, it just means we're going to try to get it to work.

When we need to, we can switch on Pkg.VERSION to check the package naming of the server, thus allowing us
to write code specific for each version.

 */
public class ConsolesLoader extends JavaPlugin {

	// This is the version we compile against, which should be the latest spigot/CB build.
	private static final String COMPILED_VERSION = "v1_8_R2";

	{
		WrappedPluginLoader loader = WrappedPluginLoader.inject(this,
				new MinecraftVersionModifier(this, COMPILED_VERSION));
		Pkg.VERSION = getServer().getClass().getPackage().getName();
		Pkg.VERSION = Pkg.VERSION.substring(Pkg.VERSION.lastIndexOf('.') + 1);
		try {
			Plugin plugin = loader.loadPlugin(this.getFile());
			loader.enablePlugin(plugin);
		} catch (InvalidPluginException e) {
			getLogger().severe("Failed to load underlying Consoles plugin!");
			e.printStackTrace();
		}
	}
}
