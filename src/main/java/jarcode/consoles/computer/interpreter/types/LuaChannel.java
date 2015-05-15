package jarcode.consoles.computer.interpreter.types;

import org.luaj.vm2.LuaError;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

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

	public void append(String content) {
		list.add(content);
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
	public void destroy() {
		destroy.run();
	}
}
