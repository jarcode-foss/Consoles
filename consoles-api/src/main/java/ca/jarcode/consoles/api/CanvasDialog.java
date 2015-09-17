package ca.jarcode.consoles.api;

public abstract class CanvasDialog implements CanvasComponent, CanvasPainter, WrappedComponent, RootComponent {

	public static Builder INTERNAL_BUILDER = null;

	public static CanvasDialog create(String text, CanvasComponent... children) {
		return INTERNAL_BUILDER.build(text, children);
	}

	public interface Builder {
		CanvasDialog build(String text, CanvasComponent... children);
	}

	protected final String text;
	protected final CanvasComponent[] children;

	public CanvasDialog(String text, CanvasComponent... children) {
		this.text = text;
		this.children = children;
	}
}
