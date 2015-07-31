package ca.jarcode.consoles.api;

import ca.jarcode.consoles.internal.ConsoleFeed;
import ca.jarcode.consoles.internal.ConsoleRenderer;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A special text component that can stream data from input and output streams straight
 * to the component itself, and offers functionality for creating 'stream factories', which
 * can be used to create a prompt.
 *
 */
public class CanvasFeed extends AbstractTextArea<ConsoleFeed> {

	public CanvasFeed(int w, int h) {
		super(w, h);
	}

	@Override
	ConsoleFeed build(ConsoleRenderer renderer) {
		return new ConsoleFeed(w, h, renderer);
	}

	/**
	 * Sets the prompt that should be shown when the feed I/O factory can be written to.
	 *
	 * @param prompt
	 */
	public void setPrompt(String prompt) {
		underlying.setPrompt(prompt);
	}

	/**
	 * Sets whether the prompt should be shown when processing text input through the factory
	 *
	 * @param show
	 */
	public void setShowPrompt(boolean show) {
		underlying.setShowPrompt(show);
	}

	/**
	 * Sets the I/O streams for this feed component.
	 *
	 * @param in the input stream to use
	 * @param out the output stream to use
	 * @param encoding the encoding to use
	 */
	public void setIO(InputStream in, OutputStream out, Encoding encoding) {
		if (underlying.hasEnded()) {
			underlying.setIO(in, out, encoding.underlying);
			underlying.startFeed();
		}
	}

	/**
	 * Sends a signal to stop the current feed
	 */
	public void stopFeed() {
		underlying.stop();
	}

	/**
	 * Forwards data to this feed. If there is an open I/O stream being fed to this component, then
	 * the data is written to the output stream. If there if no open streams, then it is written to
	 * component and handled through the I/O factory (only if the factory exists).
	 *
	 * @param data the data (in text) to write to the feed
	 */
	public void write(String data) {
		underlying.write(data);
	}

	/**
	 * Sets the I/O factory to use for this feed, which is used to create new streams if there is no
	 * current stream available to write to.
	 *
	 * @param factory the IOFactory to use
	 */
	public void setFactory(IOFactory factory) {
		underlying.setFeedCreator(new ConsoleFeed.FeedCreator() {
			IOFactoryResult result = null;
			@Override
			public void from(String input) {
				result = factory.process(input);
			}

			@Override
			public String result() {
				return result.isValid() ? (result.errorMessage() == null ? "" : result.errorMessage()) : null;
			}

			@Override
			public InputStream getInputStream() {
				return result.input();
			}

			@Override
			public OutputStream getOutputStream() {
				return result.output();
			}

			@Override
			public ConsoleFeed.FeedEncoder getEncoder() {
				return result.encoding().underlying;
			}
		});
	}
	public enum Encoding {

		UTF_8(ConsoleFeed.UTF_ENCODER);

		ConsoleFeed.FeedEncoder underlying;
		Encoding(ConsoleFeed.FeedEncoder underlying) {
			this.underlying = underlying;
		}
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
