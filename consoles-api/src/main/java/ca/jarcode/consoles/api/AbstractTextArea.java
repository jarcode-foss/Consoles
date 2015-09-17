package ca.jarcode.consoles.api;

abstract class AbstractTextArea<T> extends AbstractWrappedComponent<T> implements TextComponent {

	public AbstractTextArea(int w, int h) {
		super(w, h);
	}
}
