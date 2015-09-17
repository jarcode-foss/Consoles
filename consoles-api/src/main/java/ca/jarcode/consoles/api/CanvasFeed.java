package ca.jarcode.consoles.api;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A special text component that can stream data from input and output streams straight
 * to the component itself, and offers functionality for creating 'stream factories', which
 * can be used to create a prompt.
 *
 */
public abstract class CanvasFeed<T> extends AbstractTextArea<T> {

	public static Builder INTERNAL_BUILDER = null;

	public static CanvasFeed create(int w, int h) {
		return INTERNAL_BUILDER.build(w, h);
	}

	public interface Builder {
		CanvasFeed build(int w, int h);
	}

	public CanvasFeed(int w, int h) {
		super(w, h);
	}

	/**
	 * Sets the prompt that should be shown when the feed I/O factory can be written to.
	 *
	 * @param prompt
	 */
	public abstract void setPrompt(String prompt);

	/**
	 * Sets whether the prompt should be shown when processing text input through the factory
	 *
	 * @param show
	 */
	public abstract void setShowPrompt(boolean show);

	/**
	 * Sets the I/O streams for this feed component.
	 *
	 * @param in the input stream to use
	 * @param out the output stream to use
	 * @param encoding the encoding to use
	 */
	public abstract void setIO(InputStream in, OutputStream out, Encoding encoding);

	/**
	 * Sends a signal to stop the current feed
	 */
	public abstract void stopFeed();

	/**
	 * Forwards data to this feed. If there is an open I/O stream being fed to this component, then
	 * the data is written to the output stream. If there if no open streams, then it is written to
	 * component and handled through the I/O factory (only if the factory exists).
	 *
	 * @param data the data (in text) to write to the feed
	 */
	public abstract void write(String data);

	/**
	 * Sets the I/O factory to use for this feed, which is used to create new streams if there is no
	 * current stream available to write to.
	 *
	 * @param factory the IOFactory to use
	 */
	public abstract void setFactory(IOFactory factory);

	public enum Encoding {
		UTF_8
	}
	public interface IOFactory {
		IOFactoryResult process(String input);
	}
	public interface IOFactoryResult {
		boolean isValid();
		String errorMessage();
		InputStream input();
		OutputStream output();
		Encoding encoding();
	}
}
