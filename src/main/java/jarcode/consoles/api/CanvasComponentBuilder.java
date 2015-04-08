package jarcode.consoles.api;

import jarcode.consoles.ConsoleComponent;
import jarcode.consoles.ConsoleRenderer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Component builder for easy construction of custom canvas components
 */
public class CanvasComponentBuilder {

	Canvas canvas;
	int width, height;
	Byte background = null;
	boolean enabled = true;
	List<CanvasPainter> painters = new ArrayList<>();
	List<CanvasInteractListener> listeners = new ArrayList<>();
	List<Consumer<CanvasComponent>> constructors = new ArrayList<>();

	CanvasComponentBuilder(Canvas canvas, int width, int height) {
		this.canvas = canvas;
		this.width = width;
		this.height = height;
	}

	/**
	 * Sets the background color for this component
	 *
	 * @param bg the background color
	 * @return this builder
	 */
	public CanvasComponentBuilder background(byte bg) {
		background = bg;
		return this;
	}

	/**
	 * Sets whether the component is enabled or disabled by default
	 *
	 * @param enabled enabled flag
	 * @return this builder
	 */
	public CanvasComponentBuilder enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	/**
	 * Registers a painter for this component. Painters are called in the order
	 * that they are registered.
	 *
	 * @param painter the painter to register
	 * @return this builder
	 */
	public CanvasComponentBuilder painter(CanvasPainter painter) {
		painters.add(painter);
		return this;
	}

	/**
	 * Registers a click listener for this component. Listeners are called in the
	 * order that they are registered.
	 *
	 * @param listener
	 * @return
	 */
	public CanvasComponentBuilder listen(CanvasInteractListener listener) {
		listeners.add(listener);
		return this;
	}

	/**
	 * Registers construction code to be ran when the component is first created.
	 *
	 * @param consumer a {@link java.util.function.Consumer} to be invoked
	 * @return this builder
	 */
	public CanvasComponentBuilder construct(Consumer<CanvasComponent> consumer) {
		constructors.add(consumer);
		return this;
	}

	/**
	 * Creates the component
	 *
	 * @return a new canvas component
	 */
	public CanvasComponent create() {
		return new ConsoleComponent(width, height, (ConsoleRenderer) canvas) {
			{
				if (background != null)
					setBackground(background);
				setEnabled(enabled);
				constructors.stream().forEach(listeners -> listeners.accept(this));
			}
			@Override
			public void paint(CanvasGraphics g, String context) {
				painters.stream().forEach(painter -> painter.paint(g, context));
			}
			@Override
			public void handleClick(int x, int y, Player player) {
				listeners.stream().forEach(listener -> listener.handle(x, y, player));
			}
		};
	}

	/**
	 * Switches this builder to a {@link jarcode.consoles.api.CanvasContainerBuilder} for
	 * building a custom container.
	 *
	 * @return a {@link jarcode.consoles.api.CanvasContainerBuilder}
	 */
	public CanvasContainerBuilder container() {
		return new CanvasContainerBuilder(this);
	}
}
