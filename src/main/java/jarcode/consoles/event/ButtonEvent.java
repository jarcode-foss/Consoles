package jarcode.consoles.event;

import jarcode.consoles.ConsoleButton;

public class ButtonEvent extends ConsoleEvent<ConsoleButton> {
	private boolean on;
	public ButtonEvent(ConsoleButton context, boolean on) {
		super(context);
		this.on = on;
	}
	public boolean wasToggledOn() {
		return on;
	}
	public boolean wasToggledOff() {
		return !on;
	}
}
