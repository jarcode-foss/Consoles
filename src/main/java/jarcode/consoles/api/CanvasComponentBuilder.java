package jarcode.consoles.api;

import jarcode.consoles.ConsoleComponent;
import jarcode.consoles.ConsoleRenderer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
	public CanvasComponentBuilder background(byte bg) {
		background = bg;
		return this;
	}
	public CanvasComponentBuilder enabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}
	public CanvasComponentBuilder painter(CanvasPainter painter) {
		painters.add(painter);
		return this;
	}
	public CanvasComponentBuilder listen(CanvasInteractListener listener) {
		listeners.add(listener);
		return this;
	}
	public CanvasComponentBuilder construct(Consumer<CanvasComponent> consumer) {
		constructors.add(consumer);
		return this;
	}
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
	public CanvasContainerBuilder container() {
		return new CanvasContainerBuilder(this);
	}
}
