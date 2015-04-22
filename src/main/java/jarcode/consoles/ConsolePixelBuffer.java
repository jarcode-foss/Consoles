package jarcode.consoles;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;

public class ConsolePixelBuffer {

	// painting width and height. I doubt this will change.
	public static final int SIZE = 128;

	// segmented buffers for every context
	HashMap<String, byte[][][]> buffers = new HashMap<>();
	// flipped when a single map needs to update
	// triggered on forced repaints/updates and when a pixel in this map is modified
	HashMap<String, UpdateSwitch[][]> switches = new HashMap<>();
	// flipped when the buffer needs to be repainted for a context
	HashMap<String, Boolean> paintSwitches = new HashMap<>();
	// player listener, used to trigger updates on certain events
	private PlayerListener listener;
	// console renderer this belongs to
	private ConsoleRenderer renderer;
	// width and height
	private int w, h;

	public ConsolePixelBuffer(ConsoleRenderer renderer, int w, int h) {
		this.w = w;
		this.h = h;
		this.renderer = renderer;
	}
	void onCreate() {
		listener = new PlayerListener();
		Bukkit.getPluginManager().registerEvents(listener, Consoles.getInstance());
		for (Player player : Bukkit.getOnlinePlayers()) {
			updateFor(player, true, true);
		}
	}
	private byte[][][] newBuffer() {
		return new byte[w][h][SIZE * SIZE];
	}
	private void initSwitches(String str) {
		UpdateSwitch[][] updated = new UpdateSwitch[w][h];
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				updated[i][j] = new UpdateSwitch();
			}
		}
		switches.put(str, updated);
	}
	public byte get(int x, int y, String context) {
		byte[][][] buffer = buffers.get(context);
		if (buffer == null)
			return 0;
		if (x / SIZE >= 0 && x / SIZE < this.w && y / SIZE >= 0 && y / SIZE < this.h)
			return buffer[x / SIZE][y / SIZE][x % SIZE + ((y % SIZE) * SIZE)];
		else return 0;
	}
	public void set(int x, int y, byte b, String context) {
		if (!buffers.containsKey(context))
			buffers.put(context, newBuffer());
		if (!switches.containsKey(context))
			initSwitches(context);
		byte[][][] buffer = buffers.get(context);
		UpdateSwitch[][] updated = switches.get(context);
		if (x >= 0 && x < this.w * SIZE && y >= 0 && y < this.h * SIZE) {
			buffer[x / SIZE][y / SIZE][x % SIZE + ((y % SIZE) * SIZE)] = b;
			updated[x / SIZE][y / SIZE].fire();
		}
	}
	static class UpdateSwitch {

		private boolean fired = false;
		public void fire() {
			fired = true;
		}
		public boolean wasFired() {
			boolean b = fired;
			fired = false;
			return b;
		}
	}
	boolean needsRepaint(String context) {
		return paintSwitches.containsKey(context) ? paintSwitches.get(context) : true;
	}
	void switchRepaint(String context, boolean repaint) {
		paintSwitches.put(context, repaint);
	}
	void callRepaint() {
		paintSwitches.clear();
	}
	byte[] getBuffer(String context, int x, int y) {
		return !buffers.containsKey(context) ?
				null : buffers.get(context)[x][y];
	}
	public void resetSwitches(String context) {
		if (!switches.containsKey(context)) return;
		UpdateSwitch[][] updated = new UpdateSwitch[w][h];
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < h; j++) {
				updated[i][j] = new UpdateSwitch();
				updated[i][j].fire();
			}
		}
		switches.put(context, updated);
	}
	ConsolePixelBuffer.UpdateSwitch getSwitch(String context, int x, int y) {
		return !switches.containsKey(context) ?
				null : switches.get(context)[x][y];
	}
	boolean contextExists(String context) {
		return switches.containsKey(context);
	}
	public void updateFor(Player player, boolean force, boolean paintIfNew) {
		ConsoleHandler.getInstance().getPainter().updateFor(renderer, player, force, paintIfNew);
	}

	private class PlayerListener implements Listener {
		// track whether player is in range or not
		private boolean inside = false;
		@EventHandler
		@SuppressWarnings("unused")
		// We take the join events and update a player's paintings after
		// This way, when a player moves within range of the console,
		// this painting is updated for them (because they haven't had it rendered since they joined!)
		public void onPlayerJoin(final PlayerJoinEvent e) {
			// delay initial map packets
			Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
				if (renderer.pos.getWorld() == e.getPlayer().getWorld()
						&& renderer.pos.distanceSquared(e.getPlayer().getLocation()) <= 1280) {
					updateFor(e.getPlayer(), true, true);
				}
			}, 10L);
		}
		@EventHandler
		@SuppressWarnings("unused")
		public void onPlayerQuit(PlayerQuitEvent e) {
			for (ConsoleMapRenderer map : renderer.renderers()) {
				map.clearContextCache(e.getPlayer().getName());
			}
		}
		// update the painting for players that just entered the area
		@EventHandler
		@SuppressWarnings("unused")
		public void onPlayerMove(PlayerMoveEvent e) {
			boolean last = inside;
			inside = renderer.pos.getWorld() == e.getPlayer().getWorld()
					&& renderer.pos.distanceSquared(e.getTo()) <= 1280;
			// if the player entered the radius, update the painting
			if (!last && inside)
				updateFor(e.getPlayer(), false, true);
		}

		public void remove() {
			HandlerList.unregisterAll(this);
		}
	}
	void remove() {
		listener.remove();
	}
}
