package ca.jarcode.consoles;

import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

import static ca.jarcode.consoles.Lang.lang;

public class NMSHandler {

	private static final Map<String, Runnable> HOOKS = new HashMap<>();

	private static String VERSION = "?";

	static {
		HOOKS.put("v1_8_R3", ca.jarcode.consoles.v1_8_R3.NMSModule::link);
		HOOKS.put("v1_8_R2", ca.jarcode.consoles.v1_8_R2.NMSModule::link);
	}

	private static void unsupported() {
		throw new RuntimeException(String.format(lang.getString("unsupported"), VERSION));
	}

	public static void init() {
		String version = Bukkit.getServer().getClass().getPackage().getName();
		VERSION = version.substring(version.lastIndexOf('.') + 1);
		if (!HOOKS.containsKey(VERSION))
			unsupported();
		HOOKS.get(VERSION).run();
	}
}
