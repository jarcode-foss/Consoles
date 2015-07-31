package ca.jarcode.consoles.api;

import ca.jarcode.consoles.internal.ConsoleComponent;
import ca.jarcode.consoles.internal.ConsoleRenderer;
import org.bukkit.entity.Player;

/**
 * Class used internally to handle wrapped components in the canvas API.
 *
 * @param <T> the type of component to wrap
 */
abstract class AbstractWrappedComponent<T extends ConsoleComponent> implements CanvasComponent, WrappedComponent, CanvasPainter, PreparedComponent {

	protected T underlying = null;

	protected int w, h;

	public AbstractWrappedComponent(int w, int h) {
		this.w = w;
		this.h = h;
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
	public void prepare(ConsoleRenderer renderer) {
		underlying = build(renderer);
	}

	@Override
	public ConsoleComponent underlying() {
		return underlying;
	}

	/**
	 * Builds the type of component that this class is wrapping, with the given renderer
	 *
	 * @param renderer the renderer to build the component against
	 * @return an instance of this component's type
	 */
	abstract T build(ConsoleRenderer renderer);
}
