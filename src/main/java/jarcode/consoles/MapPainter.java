package jarcode.consoles;

import net.minecraft.server.v1_8_R1.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MapPainter implements Runnable {

	private final Object LOCK = new Object();
	private ArrayList<StackEntry> stack = new ArrayList<>();
	private volatile boolean running = true;

	// only in paint thread
	private String context;

	@Override
	public void run() {
		while (running) {

			long at = System.currentTimeMillis();
			ArrayList<StackEntry> stack;
			synchronized (LOCK) {
				if (System.currentTimeMillis() - at > 20)
					System.out.println("Warning, took more than 20ms to obtain rendering lock for painting thread!");

				while (this.stack.isEmpty()) {
					try {
						LOCK.wait();
					} catch (InterruptedException ignored) {}
				}
				stack = this.stack;
				this.stack = new ArrayList<>();
			}

			HashMap<ConsoleRenderer, List<String>> painted = new HashMap<>();
			for (StackEntry entry : stack) {
				ConsoleRenderer renderer = entry.renderer;

				// check for duplicate paint requests and ignore them
				List<String> paintedContexts = painted.get(renderer);
				if (paintedContexts == null) {
					paintedContexts = new ArrayList<>();
					painted.put(renderer, paintedContexts);
				}

				at = System.currentTimeMillis();
				synchronized (renderer.RENDERER_LOCK) {
					if (System.currentTimeMillis() - at > 20)
						System.out.println("Warning, took more than 20ms to obtain rendering lock for console!");
					if (entry.type == EntryType.TOGGLE) {
						for (String context : entry.identifiers) {
							renderer.getPixelBuffer().resetSwitches(context);
						}
					}
					else if (entry.type == EntryType.UPDATE) {
						if (renderer.created()) {
							for (int t = 0; t < entry.connections.length; t++) {
								boolean force = false;
								if ((entry.paint || (entry.paintIfNew
										&& !renderer.getPixelBuffer().contextExists(entry.identifiers[t])))
										&& !paintedContexts.contains(entry.identifiers[t])) {
										force = true;
									try {
										context = entry.identifiers[t];
										at = System.currentTimeMillis();
										renderer.paint();
										if (System.currentTimeMillis() - at > 100)
											System.out.println("Warning, took more than 100ms for paint (" +
													(System.currentTimeMillis() - at) + "), class: " + renderer.getClass() +
													", name: " + renderer.name + ", index: " + t + ", entry size: " +
													entry.connections.length + ", stack size: " + stack.size()
													+ ", identifiers: " + entry.identifiers[t]);
									}
									catch (Throwable e) {
										e.printStackTrace();
									}
									finally {
										context = null;
									}
									paintedContexts.add(entry.identifiers[t]);
								}
								at = System.currentTimeMillis();
								for (ConsoleMapRenderer map : renderer.renderers()) {
									if (entry.force || force)
										map.forceSwitch(entry.identifiers[t]);
									map.update(entry.connections[t], entry.identifiers[t]);
									if (System.currentTimeMillis() - at > 20)
										System.out.println("Warning, took more than 20ms to send packet!");
								}
							}
						}
					}
				}
			}
			stack.clear();
		}
	}

	// this is for simplifying painting code so we don't have to supply parameters in paint methods.
	// must be in paint thread. This is not synchronized to anything.
	public String getPaintContext() {
		return context;
	}

	public void stop() {
		running = false;
	}

	/**
	 * Requests a full repaint of the given console. Adjacent players will be sent packets with the new data.
	 *
	 * @param renderer the console to repaint
	 */
	public void repaint(ConsoleRenderer renderer) {

		// only render to closest players
		List<Player> close = Bukkit.getOnlinePlayers().stream()
				.filter(player -> renderer.pos.getWorld() == player.getWorld()
						&& renderer.pos.distance(player.getLocation()) <= 64)
				.collect(Collectors.toList());

		synchronized (LOCK) {
			PlayerConnection[] arr = new PlayerConnection[close.size()];
			String[] names = new String[close.size()];
			for (int t = 0; t < arr.length; t++) {
				arr[t] = (((CraftPlayer) close.get(t)).getHandle().playerConnection);
				names[t] = close.get(t).getName();
			}
			stack.add(new StackEntry(renderer, arr, names, true, false, false));
			LOCK.notify();
		}
	}

	/**
	 * Toggles all the switches for the pixel buffer in this console. This
	 * guarantees that packets will be sent the next time an update is requested.
	 *
	 * @param renderer the console to manipulate
	 * @param contexts the contexts to update
 	 */
	public void toggle(ConsoleRenderer renderer, String... contexts) {
		synchronized (LOCK) {
			stack.add(new StackEntry(renderer, contexts));
			LOCK.notify();
		}
	}

	/**
	 * Requests a painting for the given player and console.
	 *
	 * @param renderer the console to update
	 * @param player the player to send the packets to
	 */
	public void updateFor(ConsoleRenderer renderer, Player player) {
		updateFor(renderer, player, false, false);
	}

	/**
	 * Requests a painting for the given player and console.
	 *
	 * @param renderer the console to update
	 * @param player the player to send the packets to
	 * @param force forces packets to be sent, even if no pixels changed in the section(s) since it was last updated
	 * @param paintIfNew will repaint the console if it hasn't been painted before
	 */
	public void updateFor(ConsoleRenderer renderer, Player player, boolean force, boolean paintIfNew) {
		if (renderer.pos.getWorld() == player.getWorld() && renderer.pos.distance(player.getLocation()) > 64) return;

		synchronized (LOCK) {
			stack.add(new StackEntry(renderer,
					new PlayerConnection[]{(((CraftPlayer) player).getHandle().playerConnection)},
					new String[]{player.getName()}, false, force, paintIfNew));
			LOCK.notify();
		}
	}
	public Player translateContext(String context) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getName().equalsIgnoreCase(context))
				return player;
		}
		return null;
	}
	public void repaintFor(ConsoleRenderer renderer, Player player) {
		if (renderer.pos.distance(player.getLocation()) > 64) return;

		synchronized (LOCK) {
			stack.add(new StackEntry(renderer,
					new PlayerConnection[] {(((CraftPlayer) player).getHandle().playerConnection)},
					new String[] {player.getName()}, true, false, false));
			LOCK.notify();
		}
	}
	private class StackEntry {
		ConsoleRenderer renderer;
		PlayerConnection[] connections;
		String[] identifiers;
		boolean paint;
		boolean paintIfNew;
		boolean force;
		EntryType type;
		public StackEntry(ConsoleRenderer renderer, PlayerConnection[] connections,
		                  String[] identifiers, boolean paint, boolean force, boolean paintIfNew) {
			this.renderer = renderer;
			this.connections = connections;
			this.identifiers = identifiers;
			this.paint = paint;
			this.force = force;
			this.paintIfNew = paintIfNew;
			type = EntryType.UPDATE;
		}
		public StackEntry(ConsoleRenderer renderer, String... contexts) {
			this.renderer = renderer;
			this.identifiers = contexts;
			type = EntryType.TOGGLE;
		}
	}
	private enum EntryType {
		UPDATE, TOGGLE
	}
}
