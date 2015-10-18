package ca.jarcode.consoles;

import ca.jarcode.classloading.loader.MinecraftVersionModifier;
import ca.jarcode.classloading.loader.WrappedPluginLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.URISyntaxException;

// If this class is confusing, look at the loader for the core module

@SuppressWarnings("unused")
public class ComputersLoader extends JavaPlugin {

	private static final String CORE_PLUGIN_NAME = "ConsolesCore";

	private Runnable enableTask;

	private final boolean CORE_INSTALLED;

	{
		CORE_INSTALLED = Bukkit.getPluginManager().getPlugin(CORE_PLUGIN_NAME) != null;

		if (CORE_INSTALLED) {
			try {
				ComputersLoaderPassthrough.jarFile =
						new File(ComputersLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}

			WrappedPluginLoader loader = WrappedPluginLoader.inject(this, new Class[] {
							ComputersLoaderPassthrough.class
					},
					new MinecraftVersionModifier(this, ConsolesLoader.COMPILED_VERSION));
			try {
				Plugin plugin = loader.loadPlugin(this.getFile());
				enableTask = () -> loader.enablePlugin(plugin);
				ConsolesLoader.forceRegisterPlugin(plugin);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public void onEnable() {
		if (CORE_INSTALLED) {
			enableTask.run();
			ConsolesLoader.disableAndUnloadPlugin(this);
		}
		else {
			getLogger().severe("This plugin requires the core console plugin to function, please install it.");
			getPluginLoader().disablePlugin(this);
		}
	}
}
