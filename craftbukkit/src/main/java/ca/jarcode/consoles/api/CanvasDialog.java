package ca.jarcode.consoles.api;

import ca.jarcode.consoles.internal.ConsoleComponent;
import ca.jarcode.consoles.internal.ConsoleDialog;
import ca.jarcode.consoles.internal.ConsoleRenderer;
import org.bukkit.entity.Player;

/**
 * A wrapper for the dialog component.
 */
@SuppressWarnings("unused")
public final class CanvasDialog implements CanvasComponent, CanvasPainter, WrappedComponent, RootComponent {

	private ConsoleDialog underlying = null;
	private final String text;
	private final CanvasComponent[] children;

	/**
	 * Creates a dialog component. The component is not actually created until
	 * it is added to a console. This component cannot be added to containers.
	 * If you call methods from this method that query the properties of this
	 * component before it has been added to a console, a {@link java.lang.NullPointerException}
	 * will be thrown.
	 *
	 * @param text the text to display on the dialog
	 * @param children the underlying components in this dialog, usually buttons.
	 */
	public CanvasDialog(String text, CanvasComponent... children) {
		this.text = text;
		this.children = children;
	}
	@Override
	public int getWidth() {
		return underlying.getWidth();
	}

	@Override
	public int getHeight() {
		return underlying.getHeight();
	}

	@Override
	public boolean isContained() {
		return underlying.isContained();
	}

	@Override
	public byte getBackground() {
		return underlying.getBackground();
	}

	@Override
	public void setBackground(byte bg) {
		underlying.setBackground(bg);
	}

	@Override
	public boolean enabled() {
		return underlying.enabled();
	}

	@Override
	public void setEnabled(boolean enabled) {
		underlying.setEnabled(enabled);
	}

	@Override
	public void handleClick(int x, int y, Player player) {
		underlying.handleClick(x, y, player);
	}

	@Override
	public void paint(CanvasGraphics g, String context) {
		underlying.paint(g, context);
	}

	@Override
	public ConsoleComponent underlying() {
		return underlying;
	}

	@Override
	public void place(ConsoleRenderer renderer) {
		ConsoleComponent[] components = new ConsoleComponent[children.length];
		for (int t = 0; t < components.length; t++) {
			components[t] = children[t] instanceof WrappedComponent ?
					((WrappedComponent) children[t]).underlying() : (ConsoleComponent) children[t];
		}
		underlying = ConsoleDialog.show(renderer, text, components);
	}
}
