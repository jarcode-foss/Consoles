package ca.jarcode.consoles.api;

public abstract class CanvasTextArea<T> extends AbstractTextArea<T> {

	public static Builder INTERNAL_BUILDER = null;

	public static CanvasTextArea create(int w, int h) {
		return INTERNAL_BUILDER.build(w, h);
	}

	public interface Builder {
		CanvasTextArea build(int w, int h);
	}

	public CanvasTextArea(int w, int h) {
		super(w, h);
	}
}
