package jarcode.consoles.api;

import jarcode.consoles.ConsoleContainer;
import jarcode.consoles.ConsoleRenderer;
import jarcode.consoles.Position2D;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class CanvasContainerBuilder {

	List<Consumer<CanvasComponent>> adders = new ArrayList<>();
	List<CanvasComponent> toAdd = new ArrayList<>();

	Canvas canvas;
	int width, height;
	Byte background = null;
	boolean enabled = true;
	List<CanvasPainter> painters = new ArrayList<>();
	List<CanvasInteractListener> listeners = new ArrayList<>();
	List<Consumer<CanvasComponent>> constructors = new ArrayList<>();


	CanvasContainerBuilder(CanvasComponentBuilder builder) {
		this.width = builder.width;
		this.height = builder.height;
		this.enabled = builder.enabled;
		this.canvas = builder.canvas;
		this.background = builder.background;
		this.constructors = builder.constructors;
		this.listeners = builder.listeners;
		this.painters = builder.painters;
	}

	public CanvasContainerBuilder onAdd(Consumer<CanvasComponent> adder) {
		adders.add(adder);
		return this;
	}

	public CanvasContainerBuilder add(CanvasComponent component) {
		toAdd.add(component);
		return this;
	}

	public CanvasContainer create(Function<CanvasComponent, Position2D> mapper) {
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
				return mapper.apply(component);
			}

			@Override
			public void onClick(int x, int y, Player player) {
				listeners.stream().forEach(listener -> listener.handle(x, y, player));
			}

			@Override
			public void add(CanvasComponent component) {
				adders.stream().forEach(adder -> adder.accept(component));
			}
		};
	}
}
