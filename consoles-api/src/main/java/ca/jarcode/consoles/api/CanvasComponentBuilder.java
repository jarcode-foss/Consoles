package ca.jarcode.consoles.api;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Component builder for easy construction of custom canvas components
 */
public class CanvasComponentBuilder {

	// internal hook
	public static Function<CanvasComponentBuilder, CanvasComponent> INTERNAL_BUILDER = null;

	final Canvas canvas;

	final int width;
	final int height;
	Byte background = null;
	boolean enabled = true;
	final List<CanvasPainter> painters = new ArrayList<>();
	final List<CanvasInteractListener> listeners = new ArrayList<>();
	final List<Consumer<CanvasComponent>> constructors = new ArrayList<>();
	BooleanSupplier enabledSupplier = null;
	Consumer<Boolean> enabledConsumer = null;

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
	 * Sets whether the component is enabled or disabled by default,
	 * internally calling {@link CanvasComponent#setEnabled(boolean)}
	 *
	 * @param enabled enabled flag
	 * @return this builder
	 */
	public CanvasComponentBuilder enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	/**
	 * Sets the functions to be used for {@link CanvasComponent#setEnabled(boolean)} and
	 * {@link CanvasComponent#enabled}. If any of the arguments are null, the default
	 * behaviour for the respective function is used.
	 *
	 * @param supplier the supplier function
	 * @param consumer the consumer function
	 * @return this builder
	 */
	public CanvasComponentBuilder enabledHandler(BooleanSupplier supplier, Consumer<Boolean> consumer) {
		enabledSupplier = supplier;
		enabledConsumer = consumer;
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
	 * @param listener the listener to register
	 * @return this builder
	 */
	public CanvasComponentBuilder listen(CanvasInteractListener listener) {
		listeners.add(listener);
		return this;
	}

	/**
	 * Registers construction code to be ran when the component is first created.
	 *
	 * @param consumer a {@link Consumer} to be invoked
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
		return INTERNAL_BUILDER.apply(this);
	}

	/**
	 * Switches this builder to a {@link ca.jarcode.consoles.api.CanvasContainerBuilder} for
	 * building a custom container.
	 *
	 * @return a {@link ca.jarcode.consoles.api.CanvasContainerBuilder}
	 */
	public CanvasContainerBuilder container() {
		return new CanvasContainerBuilder(this);
	}

	// Getters, used in implementation. You shouldn't need to use these otherwise.

	public Canvas getCanvas() {
		return canvas;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Byte getBackground() {
		return background;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public List<CanvasPainter> getPainters() {
		return painters;
	}

	public List<CanvasInteractListener> getListeners() {
		return listeners;
	}

	public List<Consumer<CanvasComponent>> getConstructors() {
		return constructors;
	}

	public BooleanSupplier getEnabledSupplier() {
		return enabledSupplier;
	}

	public Consumer<Boolean> getEnabledConsumer() {
		return enabledConsumer;
	}
}
