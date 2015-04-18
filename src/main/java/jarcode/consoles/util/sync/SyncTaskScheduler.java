package jarcode.consoles.util.sync;

import jarcode.consoles.Consoles;
import org.bukkit.Bukkit;

import java.util.ArrayList;

public class SyncTaskScheduler implements Runnable {

	private final Object TASK_LOCK = new Object();
	private final Object THREAD_POOL_LOCK = new Object();
	private final ArrayList<Thread> POOL = new ArrayList<>();
	private final ArrayList<SyncResponseRunnable> RESPONSES = new ArrayList<>();

	private final int taskId;

	private static SyncTaskScheduler instance = null;

	public static SyncTaskScheduler create() {
		if (instance == null) {
			instance = new SyncTaskScheduler();
			return instance;
		}
		return instance;
	}

	public static SyncTaskScheduler instance() {
		return instance;
	}

	private SyncTaskScheduler() {
		taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Consoles.getInstance(), this, 0, 1);
	}

	public void end() throws InterruptedException {
		synchronized (THREAD_POOL_LOCK) {
			for (Thread thread : POOL) {
				thread.join();
			}
		}
		Bukkit.getScheduler().cancelTask(taskId);
		instance = null;
	}

	// ran in a scheduler
	public void run() {
		synchronized (TASK_LOCK) {
			RESPONSES.forEach(SyncTaskScheduler.SyncResponseRunnable::run);
			RESPONSES.clear();
		}
	}

	// method to be used whenever, from any thread
	public <T> void runSyncTask(SyncTask<T> task, SyncTaskResponse<T> response) {
		synchronized (THREAD_POOL_LOCK) {
			Thread thread = new Thread(new SyncTaskRunnable<>(task, response));
			thread.setName("Sync task");
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY); // remove thread starvation
			POOL.add(thread);
			thread.run();
		}
	}

	// ran in the main thread
	private class SyncResponseRunnable<T> implements Runnable {
		private SyncTaskResponse<T> response;
		private T responseObject;
		public SyncResponseRunnable(SyncTaskResponse<T> response, T responseObject) {
			this.response = response;
			this.responseObject = responseObject;
		}
		public void run() {
			response.run(responseObject);
		}
	}

	// ran in a separate thread
	private class SyncTaskRunnable<T> implements Runnable {
		private SyncTask<T> task;
		private SyncTaskResponse<T> response;
		public SyncTaskRunnable(SyncTask<T> task, SyncTaskResponse<T> response) {
			this.task = task;
			this.response = response;
		}
		public void run() {
			T object = task.run();
			synchronized (THREAD_POOL_LOCK) {
				POOL.remove(Thread.currentThread());
			}
			if (response != null) {
				synchronized (TASK_LOCK) {
					RESPONSES.add(new SyncResponseRunnable<>(response, object));
				}
			}
		}
	}
}
