package jarcode.consoles.computer.interpreter.types;

import org.luaj.vm2.LuaError;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unused")
public class LuaChannel {

	private List<String> list = new CopyOnWriteArrayList<>();
	private Runnable update;
	private Runnable destroy;

	private final Object LOCK = new Object();

	public LuaChannel(Runnable update, Runnable destroy) {
		this.update = update;
		this.destroy = destroy;
	}

	public void append(String content) {
		list.add(content);
		synchronized (LOCK) {
			LOCK.notify();
		}
	}
	public String poll() {
		if (list.size() == 0) return null;
		else {
			String str = list.get(0);
			list.remove(0);
			return str;
		}
	}
	public String read() {
		while (list.size() == 0) {
			try {
				synchronized (LOCK) {
					LOCK.wait();
				}
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
	public void destroy() {
		destroy.run();
	}
}
