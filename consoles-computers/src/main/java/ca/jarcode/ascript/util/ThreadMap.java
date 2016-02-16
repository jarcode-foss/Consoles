package ca.jarcode.ascript.util;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.function.*;

public class ThreadMap<V> {
	private ConcurrentHashMap<Thread, V> map = new ConcurrentHashMap<>();
	private Consumer<V> defaultPurgeOp = null;

	public void put(V value) {
		map.put(Thread.currentThread(), value);
	}

	public V get() {
		return map.get(Thread.currentThread());
	}

	public void setDefaultPurgeOperation(Consumer<V> defaultPurgeOp) {
		this.defaultPurgeOp = defaultPurgeOp;
	}

	public void purge() {
		if (defaultPurgeOp != null) {
			purge(defaultPurgeOp);
		}
	}

	public void purge(Consumer<V> onRemove) {
		for (Thread thread : map.keySet().toArray(new Thread[map.size()])) {
			if (!thread.isAlive()) {
				onRemove.accept(map.get(thread));
				map.remove(thread);
			}
		}
	}
}
