package jarcode.consoles.event.bukkit;

import jarcode.consoles.api.Console;
import jarcode.consoles.internal.ManagedConsole;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ConsoleCreateEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private ManagedConsole console;
	private BlockFace face;

	private Location location;

	private boolean cancelled;

	public ConsoleCreateEvent(ManagedConsole console, BlockFace face, Location location) {
		this.console = console;
		this.face = face;
		this.location = location;
	}

	public Location getLocation() {
		return location;
	}

	public BlockFace getFace() {
		return face;
	}

	public ManagedConsole getInternal() {
		return console;
	}

	public Console getConsole() {
		return Console.wrap(console);
	}

	public boolean isImage() {
		return getType().equals("image");
	}

	public boolean isComputer() {
		return getType().equals("computer");
	}

	public String getType() {
		return console.getType();
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	@SuppressWarnings("unused")
	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean b) {
		cancelled = b;
	}
}
