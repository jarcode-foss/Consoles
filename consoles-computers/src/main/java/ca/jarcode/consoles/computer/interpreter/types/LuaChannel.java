package ca.jarcode.consoles.computer.interpreter.types;

import ca.jarcode.consoles.computer.manual.Arg;
import ca.jarcode.consoles.computer.manual.FunctionManual;
import ca.jarcode.consoles.computer.manual.TypeManual;
import org.luaj.vm2.LuaError;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

@TypeManual(
		value = "A channel that can be opened for listening to messages sent from other computers.",
		usage = "-- Opens a new channel\n" +
				"local channel = registerChannel(\"MyChannel\")\n" +
				"-- Waits for a message to be received and reads it\n" +
				"local msg = channel:read()")
@SuppressWarnings("unused")
public class LuaChannel {

	private List<String> list = new CopyOnWriteArrayList<>();
	private Runnable update;
	private Runnable destroy;
	private BooleanSupplier terminated;

	public LuaChannel(Runnable update, Runnable destroy, BooleanSupplier terminated) {
		this.update = update;
		this.destroy = destroy;
		this.terminated = terminated;
	}

	@FunctionManual("Writes data to this channel as if a client was writing to it. This function is called " +
			"internally to send this channel messages from other clients.")
	public void append(
			@Arg(name = "content", info = "the content to append to this channel") String content) {
		list.add(content);
	}

	@FunctionManual("Polls data from this channel, returning the next available message, or nil if no message " +
			"has been received.")
	public String poll() {
		if (list.size() == 0) return null;
		else {
			String str = list.get(0);
			list.remove(0);
			return str;
		}
	}

	@FunctionManual("Reads data from this channel, blocking until the next available message. Once a message " +
			"is available, this function will return it.")
	public String read() {
		while (list.size() == 0) {
			try {
				if (terminated.getAsBoolean())
					return null;
				Thread.sleep(10);
			}
			catch (InterruptedException e) {
				throw new LuaError(e);
			}
			update.run();
		}
		String str = list.get(0);
		list.remove(0);
		return str;
	}

	@FunctionManual("Destroys this channel, cleaning up any resources and frees the channel name.")
	public void destroy() {
		destroy.run();
	}
}
