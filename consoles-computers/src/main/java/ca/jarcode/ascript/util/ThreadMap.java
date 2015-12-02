package ca.jarcode.ascript.util;

import java.util.ArrayList;
import java.util.List;

public class ThreadMap<V> {
	private List<Thread> threadList = new ArrayList<>();
	private List<V> valueList = new ArrayList<>();

	private synchronized int createEntry(Thread thread) {
		threadList.add(thread);
		valueList.add(null);
		assert threadList.size() == valueList.size();
		return threadList.size() - 1;
	}

	public synchronized void put(V value) {
		if (value == null) throw new NullPointerException();
		Thread current = Thread.currentThread();
		int idx = threadList.indexOf(current);
		if (idx == -1) {
			idx = createEntry(current);
		}
		valueList.set(idx, value);
	}

	public synchronized V get() {
		Thread current = Thread.currentThread();
		int idx = threadList.indexOf(current);
		return idx == -1 ? null : valueList.get(idx);
	}

	public synchronized void purge() {
		for (int t = 0; t < threadList.size(); t++) {
			if (!threadList.get(t).isAlive()) {
				threadList.remove(t);
				valueList.remove(t);
			}
		}
		assert threadList.size() == valueList.size();
	}
}
