package ca.jarcode.consoles.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Container builder for easy construction of custom containers
 */
public class CanvasContainerBuilder {

	// internal hook
	public static Function<CanvasContainerBuilder, CanvasContainer> INTERNAL_BUILDER = null;

	final List<Consumer<CanvasComponent>> adders = new ArrayList<>();
	final List<CanvasComponent> toAdd = new ArrayList<>();

	final Canvas canvas;
	final int width;
	final int height;
	Byte background = null;

	boolean enabled = true;
	List<CanvasPainter> painters = new ArrayList<>();
	List<CanvasInteractListener> listeners = new ArrayList<>();
	List<Consumer<CanvasComponent>> constructors = new ArrayList<>();
	Function<CanvasComponent, Position2D> mapper = null;
	BooleanSupplier enabledSupplier = null;
	Consumer<Boolean> enabledConsumer = null;

	CanvasContainerBuilder(CanvasComponentBuilder builder) {
		this.width = builder.width;
		this.height = builder.height;
		this.enabled = builder.enabled;
		this.canvas = builder.canvas;
		this.background = builder.background;
		this.constructors = builder.constructors;
		this.listeners = builder.listeners;
		this.painters = builder.painters;
		this.enabledConsumer = builder.enabledConsumer;
		this.enabledSupplier = builder.enabledSupplier;
	}

	/**
	 * Handles the passed functionality for each component added to this container.
	 * Multiple consumers can be passed and handled for each component
	 *
	 * @param adder A {@link Consumer} that handles
	 * @return this container builder
	 */
	public CanvasContainerBuilder onAdd(Consumer<CanvasComponent> adder) {
		adders.add(adder);
		return this;
	}

	/**
	 * Schedules the given component to be added when the container is created
	 *
	 * @param component the component to add
	 * @return this container builder
	 */
	public CanvasContainerBuilder add(CanvasComponent component) {
		toAdd.add(component);
		return this;
	}

	/**
	 * Sets the mapper that assigns underlying components to positions in this container.
	 * Positions are relevant to the container's origin. This function is used to
	 * resolve the absolute positions of added components when handling click events.
	 *
	 * @param mapper the component mapper to use
	 * @return this container builder
	 */
	public CanvasContainerBuilder position(Function<CanvasComponent, Position2D> mapper) {
		this.mapper = mapper;
		return this;
	}

	/**
	 * Creates the container
	 *
	 * @return a new {@link ca.jarcode.consoles.api.CanvasContainer} object
	 */
	public CanvasContainer create() {
		return INTERNAL_BUILDER.apply(this);
	}

	// Getters, used in implementation. You shouldn't need to use these otherwise.

	public List<Consumer<CanvasComponent>> getAdders() {
		return adders;
	}

	public List<CanvasComponent> getToAdd() {
		return toAdd;
	}

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

	public Function<CanvasComponent, Position2D> getMapper() {
		return mapper;
	}

	public BooleanSupplier getEnabledSupplier() {
		return enabledSupplier;
	}

	public Consumer<Boolean> getEnabledConsumer() {
		return enabledConsumer;
	}
}
