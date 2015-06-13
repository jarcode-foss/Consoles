package jarcode.consoles.internal;

import jarcode.consoles.Consoles;
import jarcode.consoles.util.Position2D;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*

An extension of a normal text area, this has significant changes:

- Reads input from an input stream in a separate thread
- Writes to either a FeedCreator, or the current output stream (if active)

This class is relatively abstract, while this is the superclass of Terminal,
you can use this for steaming any I/O.

 */
@SuppressWarnings("unused")
public class ConsoleFeed extends ConsoleTextArea implements Runnable {

	public static final FeedEncoder UTF_ENCODER = new FeedEncoder() {

		final Charset charset = Charset.forName("UTF-8");

		@Override
		public String get(byte[] b) {
			return new String(b, charset);
		}
		@Override
		public byte[] encode(String text) {
			return text.getBytes(charset);
		}
	};

	protected InputStream in = null;
	protected OutputStream out = null;

	private Thread feed;

	// whether the IO thread is running
	protected volatile boolean running = false;
	// whether the IO thread has ended
	protected volatile boolean ended = true;
	private Exception exception = null;
	protected final Object LOCK = new Object();
	protected FeedEncoder encoder = null;

	protected volatile String prompt = null;
	private volatile boolean showPrompt = true;

	private FeedCreator creator = null;
	protected boolean initialized = false;

	private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	protected ByteArrayOutputStream outgoing = new ByteArrayOutputStream();

	private final Object TASK_LOCK = new Object();
	private final List<Integer> tasks = new ArrayList<>();

	private final List<Runnable> afterTasks = new ArrayList<>();

	public ConsoleFeed(ConsoleRenderer renderer) {
		super(renderer.getWidth() - 4, renderer.getHeight() - 4, renderer);
	}
	public ConsoleFeed(int x, int y, ConsoleRenderer renderer) {
		super(x, y, renderer);
	}
	public void place() {
		getRenderer().putComponent(new Position2D(2, 2), this);
	}
	public void setIO(InputStream in, OutputStream out, FeedEncoder encoder) {
		this.in = in;
		this.out = out;
		this.encoder = encoder;
		initialized = true;
	}
	public void setFeedCreator(FeedCreator creator) {
		this.creator = creator;
	}
	public void setPrompt(String prompt) {
		synchronized (LOCK) {
			this.prompt = prompt;
		}
	}
	public void prompt() {
		if (!showPrompt) return;
		if (getLastLine().length() > 0)
			advanceLine();
		print(prompt);
	}
	public void startFeed() {
		if (running && !ended)
			throw new IllegalStateException("Feed is already set up to IO");
		running = true;
		ended = false;
		feed = new Thread(this);
		feed.setName("Minecraft Console IO Feed");
		feed.setDaemon(true);
		feed.setPriority(Thread.MIN_PRIORITY);
		feed.start();
	}
	public void setShowPrompt(boolean show) {
		showPrompt = show;
	}
	public Thread getFeedThread() {
		return feed;
	}
	public void stop() {
		running = false;
	}
	public boolean hasEnded() {
		return ended;
	}
	public Exception getException() {
		synchronized (LOCK) {
			return exception;
		}
	}
	@Override
	public void onRemove() {
		stop();
	}
	public boolean isBusy() {
		return !(creator != null && ended);
	}
	// non-blocking
	public void write(String string) {
		if (creator != null && ended) {
			print(string);
			creator.from(string);
			String result = creator.result();
			advanceLine();
			if (result == null) {
				setIO(creator.getInputStream(), creator.getOutputStream(), creator.getEncoder());
				startFeed();
			}
			else {
				if (!result.isEmpty())
					println(result);
				if (prompt != null && showPrompt)
					print(prompt);
			}
		}
		else if (initialized && running && !ended) {
			try {
				if (out != null) synchronized (LOCK) {
					byte[] arr = encoder.encode(string + "\n");
					outgoing.write(arr);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			advanceLine();
			print("Could not forward input to terminal (creator=" + creator + ", is=" + in + ", os=" + out + ")," +
					" running: " + running + ", ended: " + ended + ", initialized: " + initialized);
			if (showPrompt) {
				advanceLine();
				print(prompt);
			}
		}
		repaint();
	}

	@Override
	public ConsoleMessageListener createListener() {
		return (sender, text) -> {
			write(text);
			return "Sent to console";
		};
	}

	@Override
	public void run() {
		try {
			while (running) {
				synchronized (LOCK) {
					if (outgoing.size() > 0 && out != null) {
						out.write(outgoing.toByteArray());
						outgoing = new ByteArrayOutputStream();
					}
					boolean eof = false;
					while (in.available() > 0) {
						int v = in.read();
						if (v == -1) {
							eof = true;
							break;
						}
						else
							buffer.write(v);
					}
					if (buffer.size() > 0) {
						String result = encoder.get(buffer.toByteArray());
						if (result != null)
							writeConsole(result);
						buffer = new ByteArrayOutputStream();
					}
					if (eof) {
						break;
					}
				}
				Thread.sleep(50);
			}
		}
		catch (Exception e) {
			synchronized (LOCK) {
				if (Consoles.debug)
					e.printStackTrace();
				exception = e;
			}
		}
		finally {
			ended = true;
			running = false;
			synchronized (LOCK) {
				if (prompt != null && showPrompt) {
					writeConsole("\n" + prompt);
				}
			}
			waitFor();
			Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
				Arrays.asList(afterTasks.stream().toArray(Runnable[]::new))
						.stream().forEach(Runnable::run);
				afterTasks.clear();
			});
		}
	}
	private void writeConsole(String text) {
		if (Consoles.getInstance().isEnabled()) {
			AtomicInteger id = new AtomicInteger(-1);
			id.set(Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
				this.print(text);
				repaint();
				synchronized (TASK_LOCK) {
					tasks.remove((Integer) id.get());
					TASK_LOCK.notify();
				}
			}));
			synchronized (TASK_LOCK) {
				tasks.add(id.get());
			}
		}
	}

	// should be used by the main thread instead of waitFor()
	public void doAfter(Runnable task) {
		afterTasks.add(task);
	}

	// should not be called by the main thread, as the tasks themselves
	// are ran from it.
	public void waitFor() {
		synchronized (TASK_LOCK) {
			try {
				while (tasks.size() > 0 || !ended) {
					TASK_LOCK.wait();
				}
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	public interface FeedEncoder {
		public String get(byte[] read);
		public byte[] encode(String text);
	}
	public interface FeedCreator {
		public void from(String input);
		public String result();
		public InputStream getInputStream();
		public OutputStream getOutputStream();
		public FeedEncoder getEncoder();
	}
}
