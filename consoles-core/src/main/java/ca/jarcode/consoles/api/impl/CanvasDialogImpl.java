package ca.jarcode.consoles.api.impl;

import ca.jarcode.consoles.api.*;
import ca.jarcode.consoles.internal.ConsoleComponent;
import ca.jarcode.consoles.internal.ConsoleDialog;
import ca.jarcode.consoles.internal.ConsoleRenderer;
import org.bukkit.entity.Player;

/**
 * A wrapper for the dialog component.
 */
@SuppressWarnings("unused")
public final class CanvasDialogImpl extends CanvasDialog {

	private ConsoleDialog underlying = null;

	/**
	 * Creates a dialog component. The component is not actually created until
	 * it is added to a console. This component cannot be added to containers.
	 * If you call methods from this method that query the properties of this
	 * component before it has been added to a console, they will be ignored.
	 *
	 * @param text the text to display on the dialog
	 * @param children the underlying components in this dialog, usually buttons.
	 */
	public CanvasDialogImpl(String text, CanvasComponent... children) {
		super(text, children);
	}
	@Override
	public int getWidth() {
		return underlying == null ? -1 : underlying.getWidth();
	}

	@Override
	public int getHeight() {
		return underlying == null ? -1 : underlying.getHeight();
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
	public Object underlying() {
		return underlying;
	}

	@Override
	public void place(Canvas c) {
		ConsoleRenderer renderer = (ConsoleRenderer) c;
		ConsoleComponent[] components = new ConsoleComponent[children.length];
		for (int t = 0; t < components.length; t++) {
			components[t] = children[t] instanceof WrappedComponent ?
					(ConsoleComponent) ((WrappedComponent) children[t]).underlying() : (ConsoleComponent) children[t];
		}
		underlying = ConsoleDialog.show(renderer, text, components);
	}
}
