package ca.jarcode.consoles.api;
import org.bukkit.entity.Player;

/**
 * An implementable class for custom containers. This object acts as
 * a wrapper that builds the underlying container when it is added
 * to a canvas.
 */
public abstract class CustomContainer implements CanvasPainter, CanvasContainer, CanvasComponent,
		WrappedComponent, PreparedComponent {

	protected final int width, height;
	private CanvasContainer component;

	public CustomContainer(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public final void add(CanvasComponent component) {
		this.component.add(component);
	}

	@Override
	public final void remove(CanvasComponent comp) {
		this.component.remove(comp);
	}

	public final void construct(CanvasComponent component) {
		this.component = (CanvasContainer) component;
		onCreate(component);
	}

	@Override
	public final void prepare(Canvas renderer) {
		build(renderer);
	}

	public final CanvasContainer getComponent() {
		return component;
	}

	public abstract void onCreate(CanvasComponent component);

	public abstract byte background();

	public abstract void handleClick(int x, int y, Player player);

	public abstract boolean enabled();

	@Override
	public final int getWidth() {
		return ((CanvasComponent) component).getWidth();
	}

	@Override
	public final int getHeight() {
		return ((CanvasComponent) component).getHeight();
	}

	@Override
	public final boolean isContained() {
		return ((CanvasComponent) component).isContained();
	}

	@Override
	public final byte getBackground() {
		return ((CanvasComponent) component).getBackground();
	}

	@Override
	public final void setBackground(byte bg) {
		((CanvasComponent) component).setBackground(bg);
	}

	public abstract void setEnabled(boolean enabled);

	public abstract void handleAddedComponent(CanvasComponent comp);

	public abstract Position2D mapComponentPosition(CanvasComponent comp);

	public abstract void paint(CanvasGraphics g, String context);

	@Override
	public final CanvasComponent[] components() {
		return component.components();
	}

	@Override
	public final Object underlying() {
		return component;
	}

	@Override
	public final void onClick(int x, int y, Player player) {
		component.onClick(x, y, player);
	}
	private void build(Canvas canvas) {
		component = canvas.newComponent(width, height)
				.painter(this::paint)
				.construct(this::construct)
				.listen(this::handleClick)
				.background(background())
				.enabledHandler(this::enabled, this::setEnabled)
				.container()
				.onAdd(this::handleAddedComponent)
				.position(this::mapComponentPosition)
				.create();
	}
}
