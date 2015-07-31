package ca.jarcode.consoles.api;

import ca.jarcode.consoles.internal.ConsoleTextArea;
import org.bukkit.map.MapFont;

abstract class AbstractTextArea<T extends ConsoleTextArea> extends AbstractWrappedComponent<T> implements TextComponent {

	public AbstractTextArea(int w, int h) {
		super(w, h);
	}

	@Override
	public void print(String text) {
		underlying.print(text);
	}

	@Override
	public void setFont(MapFont font) {
		underlying.setFont(font);
	}

	@Override
	public void clear() {
		underlying.clear();
	}

	@Override
	public void setTextColor(byte color) {
		underlying.setDefaultTextColor(color);
	}
}
