package jarcode.consoles.api;

import jarcode.consoles.ConsoleContainer;
import jarcode.consoles.ConsoleRenderer;
import jarcode.consoles.util.Position2D;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Container builder for easy construction of custom containers
 */
public class CanvasContainerBuilder {

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
	 * @param adder A {@link java.util.function.Consumer} that handles
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
	 * @return a new {@link jarcode.consoles.api.CanvasContainer} object
	 */
	public CanvasContainer create() {
		return new ConsoleContainer(width, height, (ConsoleRenderer) canvas) {
			{
				toAdd.stream().forEach(this::add);
			}
			@Override
			public void paint(CanvasGraphics g, String context) {
				painters.stream().forEach(painter -> painter.paint(g, context));
			}

			@Override
			public Position2D getUnderlingComponentCoordinates(CanvasComponent component) {
				return mapper == null ? null : mapper.apply(component);
			}

			@Override
			public void onClick(int x, int y, Player player) {
				listeners.stream().forEach(listener -> listener.handle(x, y, player));
			}

			@Override
			public void onAdd(CanvasComponent component) {
				adders.stream().forEach(adder -> adder.accept(component));
			}

			@Override
			public boolean enabled() {
				return enabledSupplier != null ? enabledSupplier.getAsBoolean() : super.enabled();
			}

			@Override
			public void setEnabled(boolean enabled) {
				if (enabledConsumer != null)
					enabledConsumer.accept(enabled);
				else super.setEnabled(enabled);
			}
		};
	}
}
