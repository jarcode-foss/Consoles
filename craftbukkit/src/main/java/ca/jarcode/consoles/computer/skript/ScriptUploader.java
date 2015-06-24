package ca.jarcode.consoles.computer.skript;

// This class is old code, meant to provide skript integration.
// I refuse to support skript anymore, the maven repository for
// skript is down and the git repository has not seen a commit
// in a long time.

/*
import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import jarcode.consoles.Consoles;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Method;
*/

@SuppressWarnings("SpellCheckingInspection")
public class ScriptUploader implements ScriptInterface {

	/*
	private final Method LOAD_SCRIPT;
	private final Method UNLOAD_SCRIPT;

	private Skript skript;

	private File folder;

	public ScriptUploader(Plugin scriptPlugin) throws FailedHookException {

		try {
			LOAD_SCRIPT = ScriptLoader.class.getDeclaredMethod("loadScript", File.class);
			LOAD_SCRIPT.setAccessible(true);
			UNLOAD_SCRIPT = ScriptLoader.class.getDeclaredMethod("unloadScript", File.class);
			UNLOAD_SCRIPT.setAccessible(true);
		}
		catch (Throwable e) {
			throw new FailedHookException("Could not resolve 'loadScript' method!", e);
		}

		skript = (Skript) scriptPlugin;
		folder = new File(Consoles.getInstance().getDataFolder().getAbsolutePath() + File.separatorChar + "skripts");
		if (!folder.exists()) {
			if (!folder.mkdir()) {
				throw new FailedHookException("Failed to create skript folder for plugin hook!");
			}
		}
		if (!folder.isDirectory()) {
			throw new FailedHookException("Skripts folder is actually a file! Could not hook into plugin.");
		}
		ScriptLoader.loadScripts(folder);
	}

	private ScriptLoader.ScriptInfo handle(File file, State state) {
		try {
			return (ScriptLoader.ScriptInfo) (state == State.LOAD ? LOAD_SCRIPT : UNLOAD_SCRIPT).invoke(null, file);

		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void upload(String script, String identifier) {
		File file = new File(folder.getAbsolutePath() + File.separatorChar + identifier + ".sk");
		if (file.exists())
			handle(file, State.UNLOAD);
		try (FileOutputStream out = new FileOutputStream(file)) {
			PrintWriter writer = new PrintWriter(out);
			writer.print(script);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		handle(file, State.LOAD);
	}
	@Override
	public boolean isHooked() {
		return true;
	}

	private enum State {
		LOAD, UNLOAD
	}
	*/
}
