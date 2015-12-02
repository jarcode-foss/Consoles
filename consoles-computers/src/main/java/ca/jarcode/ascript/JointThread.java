package ca.jarcode.ascript;

import ca.jarcode.ascript.util.ThrowingConsumer;
import ca.jarcode.ascript.util.ThrowingFunction;
import ca.jarcode.ascript.util.ThrowingRunnable;
import ca.jarcode.ascript.util.ThrowingSupplier;

/**
 * A small utility class for running tasks in another thread
 */
public class JointThread {

	private final Thread thread;

	private volatile boolean running = true;
	private volatile Throwable lastException;
	private volatile ThrowingFunction<Object, Object, Throwable> task = null;
	private volatile Object argument = null;
	private volatile Object ret = null;
	private volatile boolean returned = false;

	private volatile Object PASSED_STATE = null;
	private final Object STATE = new Object();
	private final Object QUEUE_STATE = new Object();
	private final Object EXIT_STATE = new Object();

	JointThread() {
		thread = new Thread(this::threadEntry);
		thread.setName("Joint Thread");
		thread.setDaemon(true);
	}

	private void threadEntry() {
		while (running) {
			synchronized (STATE) {
				try {
					STATE.wait();
				}
				// if this thread is interrupted, just exit the wait and loop
				catch (InterruptedException ignored) {}
			}
			if (task != null) {
				try {
					ret = task.apply(argument);
				}
				catch (Throwable throwable) {
					lastException = throwable;
				}
				argument = null;
				PASSED_STATE = null;
				task = null;
				returned = true;
				PASSED_STATE.notifyAll();
				PASSED_STATE = null;
				QUEUE_STATE.notify();
			}
		}
		EXIT_STATE.notifyAll();
	}

	public boolean isExecuting() {
		return task != null && !returned;
	}

	@SuppressWarnings("SynchronizeOnNonFinalField")
	public void waitForFunction() {
		while (!returned) {
			synchronized (PASSED_STATE) {
				try {
					PASSED_STATE.wait();
				}
				catch (InterruptedException ignored) {}
			}
		}
	}

	public <E extends Throwable> void runVoid(ThrowingRunnable<E> function) throws E {
		run(ignored -> {
			function.run();
			return null;
		}, null);
	}

	@SuppressWarnings("unchecked")
	public <A, E extends Throwable> void runVoid(ThrowingConsumer<A, E> function, A arg) throws E {
		this.run(a -> {
			function.accept(a);
			return null;
		}, arg);
	}

	public <R, E extends Throwable> R run(ThrowingSupplier<R, E> function) throws E {
		return run(ignored -> function.get(), null);
	}

	@SuppressWarnings({"unchecked", "SynchronizeOnNonFinalField"})
	public <A, R, E extends Throwable> R run(ThrowingFunction<A, R, E> function, A arg) throws E {
		while (PASSED_STATE != null) {
			synchronized (QUEUE_STATE) {
				try {
					QUEUE_STATE.wait();
				}
				// again, just ignore and continue
				catch (InterruptedException ignored) {}
			}
		}
		PASSED_STATE = new Object();
		argument = arg;
		task = (ThrowingFunction<Object, Object, Throwable>) function;
		STATE.notify();
		while (!returned) {
			synchronized (PASSED_STATE) {
				try {
					PASSED_STATE.wait();
				}
				catch (InterruptedException ignored) {}
			}
		}
		returned = false;
		if (lastException != null) {
			ret = null;
			throw (E) lastException;
		}
		R returnValue = (R) ret;
		ret = null;
		return returnValue;
	}

	public Throwable getLastException() {
		return lastException;
	}

	public void kill() {
		task = null;
		running = false;
		thread.interrupt();
		synchronized (EXIT_STATE) {
			try {
				EXIT_STATE.wait();
			}
			catch (InterruptedException ignored) {}
		}
	}

	public boolean isAlive() {
		return thread.isAlive();
	}

	public void start() {
		if (!thread.isAlive()) {
			running = true;
			thread.start();
		}
	}

	public Thread internalThread() {
		return thread;
	}
}
