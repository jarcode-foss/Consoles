package jarcode.consoles.api;
import jarcode.consoles.ConsoleComponent;
import jarcode.consoles.ConsoleRenderer;
import jarcode.consoles.Position2D;
import org.bukkit.entity.Player;

/**
 * An implementable class for custom components. This object acts as
 * a wrapper that builds the underlying component when it is added
 * to a canvas.
 */
public abstract class CustomComponent implements CanvasPainter, CanvasComponent, WrappedComponent,
		PreparedComponent {

	protected final int width, height;
	private CanvasComponent component;

	public CustomComponent(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public final void construct(CanvasComponent component) {
		this.component = component;
		onCreate(component);
	}

	@Override
	public final void prepare(ConsoleRenderer renderer) {
		build(renderer);
	}

	public final CanvasComponent getComponent() {
		return component;
	}

	public abstract void onCreate(CanvasComponent component);

	public abstract byte background();

	public abstract void handleClick(int x, int y, Player player);

	public abstract boolean enabled();

	@Override
	public final int getWidth() {
		return component.getWidth();
	}

	@Override
	public final int getHeight() {
		return component.getHeight();
	}

	@Override
	public final boolean isContained() {
		return component.isContained();
	}

	@Override
	public final byte getBackground() {
		return component.getBackground();
	}

	@Override
	public final void setBackground(byte bg) {
		component.setBackground(bg);
	}

	public abstract void setEnabled(boolean enabled);

	public abstract void paint(CanvasGraphics g, String context);

	@Override
	public final ConsoleComponent underlying() {
		return (ConsoleComponent) component;
	}
	private void build(Canvas canvas) {
		component = canvas.newComponent(width, height)
				.painter(this::paint)
				.construct(this::construct)
				.listen(this::handleClick)
				.background(background())
				.enabledHandler(this::enabled, this::setEnabled)
				.create();
	}
}
