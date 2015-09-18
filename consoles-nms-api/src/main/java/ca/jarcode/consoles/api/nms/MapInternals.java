package ca.jarcode.consoles.api.nms;

import org.bukkit.World;

public interface MapInternals {

	void overrideMap(int id);
	void injectTypes();
	void clearVanillaMapFiles();
	Object mapItemNMS(short id);

	boolean updateSection(MapInternals.PreparedMapSection section, World world, int centerX, int centerZ,
	                                    int updateX, int updateZ, int scale);

	class PreparedMapSection {
		public final byte[] colors = new byte[128 * 128];
		public final Object LOCK = new Object();
	}
}
