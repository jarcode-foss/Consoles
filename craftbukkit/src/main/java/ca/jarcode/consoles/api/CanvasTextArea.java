package ca.jarcode.consoles.api;

import ca.jarcode.consoles.internal.ConsoleRenderer;
import ca.jarcode.consoles.internal.ConsoleTextArea;

public class CanvasTextArea extends AbstractTextArea<ConsoleTextArea> {

	public CanvasTextArea(int w, int h) {
		super(w, h);
	}

	@Override
	ConsoleTextArea build(ConsoleRenderer renderer) {
		return new ConsoleTextArea(w, h, renderer);
	}
}
