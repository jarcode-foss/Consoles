package ca.jarcode.consoles;

import ca.jarcode.classloading.loader.MinecraftVersionModifier;
import ca.jarcode.classloading.loader.WrappedPluginLoader;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

// If this class is confusing, look at the loader for the core module

@SuppressWarnings("unused")
public class ComputersLoader extends JavaPlugin {

	private Runnable enableTask;

	{
		WrappedPluginLoader loader = WrappedPluginLoader.inject(this, new Class[] {},
				new MinecraftVersionModifier(this, ConsolesLoader.COMPILED_VERSION));

		try {
			Plugin plugin = loader.loadPlugin(this.getFile());
			enableTask = () -> loader.enablePlugin(plugin);
			ConsolesLoader.forceRegisterPlugin(plugin);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}


	@Override
	public void onEnable() {
		enableTask.run();
		ConsolesLoader.disableAndUnloadPlugin(this);
	}
}
