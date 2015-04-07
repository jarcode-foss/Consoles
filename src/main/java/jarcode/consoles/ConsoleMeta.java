package jarcode.consoles;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class ConsoleMeta {

	public Location location;
	public BlockFace face;
	public int w, h;

	public ConsoleMeta() {}

	public ConsoleMeta(Location location, BlockFace face, int w, int h) {
		this.location = location;
		this.face = face;
		this.w = w;
		this.h = h;
	}
	public ManagedConsole spawnConsole() {
		ManagedConsole console = new ManagedConsole(w, h);
		console.create(face, location);
		return console;
	}
}
