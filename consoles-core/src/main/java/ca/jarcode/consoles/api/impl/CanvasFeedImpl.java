package ca.jarcode.consoles.api.impl;

import ca.jarcode.consoles.api.Canvas;
import ca.jarcode.consoles.api.CanvasFeed;
import ca.jarcode.consoles.api.CanvasGraphics;
import ca.jarcode.consoles.internal.ConsoleFeed;
import ca.jarcode.consoles.internal.ConsoleRenderer;
import org.bukkit.entity.Player;
import org.bukkit.map.MapFont;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A special text component that can stream data from input and output streams straight
 * to the component itself, and offers functionality for creating 'stream factories', which
 * can be used to create a prompt.
 *
 */
public class CanvasFeedImpl extends CanvasFeed<ConsoleFeed> {

	private static ConsoleFeed.FeedEncoder mapInternalEncoder(Encoding encoding) {
		switch (encoding) {
			case UTF_8: return ConsoleFeed.UTF_ENCODER;
			default: return null;
		}
	}

	public CanvasFeedImpl(int w, int h) {
		super(w, h);
	}

	@Override
	public ConsoleFeed build(Canvas renderer) {
		return new ConsoleFeed(w, h, (ConsoleRenderer) renderer);
	}

	@Override
	public void setPrompt(String prompt) {
		underlying.setPrompt(prompt);
	}

	@Override
	public void setShowPrompt(boolean show) {
		underlying.setShowPrompt(show);
	}

	@Override
	public void setIO(InputStream in, OutputStream out, Encoding encoding) {
		if (underlying.hasEnded()) {
			underlying.setIO(in, out, mapInternalEncoder(encoding));
			underlying.startFeed();
		}
	}

	@Override
	public void stopFeed() {
		underlying.stop();
	}

	@Override
	public void write(String data) {
		underlying.write(data);
	}


	@Override
	public int getWidth() {
		return w;
	}

	@Override
	public int getHeight() {
		return h;
	}

	@Override
	public boolean isContained() {
		return underlying == null || underlying.isContained();
	}

	@Override
	public byte getBackground() {
		return underlying == null ? 0 : underlying.getBackground();
	}

	@Override
	public void setBackground(byte bg) {
		if (underlying != null)
			underlying.setBackground(bg);
	}

	@Override
	public boolean enabled() {
		return underlying != null && underlying.enabled();
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (underlying != null)
			underlying.setEnabled(enabled);
	}

	@Override
	public void handleClick(int x, int y, Player player) {
		if (underlying != null)
			underlying.handleClick(x, y, player);
	}

	@Override
	public void paint(CanvasGraphics g, String context) {
		if (underlying != null)
			underlying.paint(g, context);
	}

	@Override
	public void prepare(Canvas renderer) {
		underlying = build(renderer);
	}

	@Override
	public Object underlying() {
		return underlying;
	}


	@Override
	public void print(String text) {
		underlying.print(text);
	}

	@Override
	public void setFont(MapFont font) {
		underlying.setFont(font);
	}

	@Override
	public void clear() {
		underlying.clear();
	}

	@Override
	public void setTextColor(byte color) {
		underlying.setDefaultTextColor(color);
	}

	/**
	 * Sets the I/O factory to use for this feed, which is used to create new streams if there is no
	 * current stream available to write to.
	 *
	 * @param factory the IOFactory to use
	 */
	@Override
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
				return mapInternalEncoder(result.encoding());
			}
		});
	}
}
