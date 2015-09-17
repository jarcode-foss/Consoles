package ca.jarcode.consoles.internal;

import ca.jarcode.consoles.Consoles;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static ca.jarcode.consoles.Lang.lang;

/*

The class that handles tasks and requests for repainting and updates

 */
public class MapPainter implements Runnable {

	private final Object LOCK = new Object();
	private ArrayList<StackEntry> stack = new ArrayList<>();
	private volatile boolean running = false;

	// only in paint thread
	private String context;

	@Override
	public void run() {
		running = true;
		while (running) {

			long at = System.currentTimeMillis();
			ArrayList<StackEntry> stack;
			synchronized (LOCK) {
				if (System.currentTimeMillis() - at > 20)
					System.out.println(lang.getString("painter-lock1"));

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
						System.out.println(lang.getString("painter-lock2"));
					if (entry.type == EntryType.TOGGLE) {
						for (String context : entry.identifiers) {
							renderer.getPixelBuffer().resetSwitches(context);
						}
					}
					else if (entry.type == EntryType.REPAINT_TOGGLE) {
						// removes all contexts in the repaint stack for the buffer
						// this makes it so that every player who walks in range of the
						// console will have have it repainted
						renderer.getPixelBuffer().callRepaint();
					}
					else if (entry.type == EntryType.UPDATE) {
						if (renderer.created()) {
							for (int t = 0; t < entry.connections.length; t++) {
								if ((
								// We only repaint if:
								// a repaint is required for this context (the content changed)

										renderer.getPixelBuffer().needsRepaint(context)

								// the update requested a repaint (for whatever reason)

										|| entry.paint

								// if this has not been painted for this player before, and the update allows painting
								// for new consoles

								        || (entry.paintIfNew && !renderer.getPixelBuffer()
										.contextExists(entry.identifiers[t])))

								// And we also do not re-repaint, so if we've already handled a paint
								// request for this context, we ignore any other ones.

										&& !paintedContexts.contains(entry.identifiers[t])

										){
									try {
										context = entry.identifiers[t];
										at = System.currentTimeMillis();
										renderer.paint();
										if (System.currentTimeMillis() - at > 100)
											Consoles.getInstance().getLogger()
													.warning(lang.getString("painter-overload") + " (" +
													(System.currentTimeMillis() - at) + "), class: "
													+ renderer.getClass() + ", name: " + renderer.type +
													", index: " + t + ", entry size: " + entry.connections.length
													+ ", stack size: " + stack.size() + ", identifiers: "
													+ entry.identifiers[t]);
									}
									catch (Throwable e) {
										e.printStackTrace();
									}
									finally {
										context = null;
									}
									paintedContexts.add(entry.identifiers[t]);
									// add this context back to our list of contexts we have already painted for
									renderer.getPixelBuffer().switchRepaint(context);
								}
								at = System.currentTimeMillis();
								for (ConsoleMapRenderer map : renderer.renderers()) {
									// if this request forces updates, toggle switches
									if (entry.force)
										map.forceSwitch(entry.identifiers[t]);
									// send packets
									map.update(entry.connections[t], entry.identifiers[t]);
								}
								if (System.currentTimeMillis() - at > 20)
									System.out.println(lang.getString("painter-packet-overload"));
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
			// repaint switch
			stack.add(new StackEntry(renderer));
			PlayerConnection[] arr = new PlayerConnection[close.size()];
			String[] names = new String[close.size()];
			for (int t = 0; t < arr.length; t++) {
				arr[t] = (((CraftPlayer) close.get(t)).getHandle().playerConnection);
				names[t] = close.get(t).getName();
			}
			// add update requests
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
		public StackEntry(ConsoleRenderer renderer) {
			this.renderer = renderer;
			type = EntryType.REPAINT_TOGGLE;
		}
	}
	private enum EntryType {
		UPDATE, TOGGLE, REPAINT_TOGGLE
	}
}
