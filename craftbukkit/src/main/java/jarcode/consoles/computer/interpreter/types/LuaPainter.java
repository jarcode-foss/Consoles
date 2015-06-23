package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.computer.Computer;

@SuppressWarnings("unused")
public class LuaPainter {

	private final Computer computer;
	private final int index;

	public LuaPainter(int index, Computer computer) {
		this.index = index;
		this.computer = computer;
	}

	public void repaint() {
		if (computer.getComponentIndex() == index) {
			computer.getConsole().repaint();
		}
	}
}
