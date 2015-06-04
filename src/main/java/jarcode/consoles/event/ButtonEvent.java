package jarcode.consoles.event;

import jarcode.consoles.ConsoleButton;
import org.bukkit.entity.Player;

public class ButtonEvent extends ConsoleEvent<ConsoleButton> {
	private final boolean on;
	private final Player player;
	public ButtonEvent(ConsoleButton context, boolean on, Player player) {
		super(context);
		this.on = on;
		this.player = player;
	}
	public boolean wasToggledOn() {
		return on;
	}
	public boolean wasToggledOff() {
		return !on;
	}
	public Player getPlayer() {
		return player;
	}
}
