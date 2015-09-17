package ca.jarcode.consoles.api.impl;

import ca.jarcode.consoles.api.Canvas;
import ca.jarcode.consoles.api.CanvasGraphics;
import ca.jarcode.consoles.api.CanvasTextArea;
import ca.jarcode.consoles.internal.ConsoleRenderer;
import ca.jarcode.consoles.internal.ConsoleTextArea;
import org.bukkit.entity.Player;
import org.bukkit.map.MapFont;

public class CanvasTextAreaImpl extends CanvasTextArea<ConsoleTextArea> {

	public CanvasTextAreaImpl(int w, int h) {
		super(w, h);
	}

	@Override
	public ConsoleTextArea build(Canvas renderer) {
		return new ConsoleTextArea(w, h, (ConsoleRenderer) renderer);
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

	@Override
	public int getWidth() {
		return w;
	}

	@Override
	public int getHeight() {
		return h;
	}

	@Override
	public boolean isContained() {
		return underlying == null || underlying.isContained();
	}

	@Override
	public byte getBackground() {
		return underlying == null ? 0 : underlying.getBackground();
	}

	@Override
	public void setBackground(byte bg) {
		if (underlying != null)
			underlying.setBackground(bg);
	}

	@Override
	public boolean enabled() {
		return underlying != null && underlying.enabled();
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (underlying != null)
			underlying.setEnabled(enabled);
	}

	@Override
	public void handleClick(int x, int y, Player player) {
		if (underlying != null)
			underlying.handleClick(x, y, player);
	}

	@Override
	public void paint(CanvasGraphics g, String context) {
		if (underlying != null)
			underlying.paint(g, context);
	}

	@Override
	public void prepare(Canvas renderer) {
		underlying = build(renderer);
	}

	@Override
	public Object underlying() {
		return underlying;
	}
}
