package ca.jarcode.consoles.api;

/**
 * Class used to handle wrapped components in the canvas API.
 *
 * @param <T> the type of component to wrap
 */
abstract class AbstractWrappedComponent<T> implements CanvasComponent, WrappedComponent, CanvasPainter, PreparedComponent {

	protected T underlying = null;

	protected int w, h;

	public AbstractWrappedComponent(int w, int h) {
		this.w = w;
		this.h = h;
	}

	/**
	 * Builds the type of component that this class is wrapping, with the given renderer
	 *
	 * @param renderer the renderer to build the component against
	 * @return an instance of this component's type
	 */
	public abstract T build(Canvas renderer);
}
