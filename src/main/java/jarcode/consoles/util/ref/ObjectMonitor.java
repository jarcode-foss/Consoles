package jarcode.consoles.util.ref;

import org.bukkit.Bukkit;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

// not in use right now, but I may use this later to monitor objects to make
// sure that the JVM is cleaning them up - helpful for finding memory leaks
// without asking server owners to use JVisualVM to monitor it themselves,
// I can just add a threshold and dump thread call stacks automatically :)
@SuppressWarnings("unused")
public class ObjectMonitor {
	private final ReferenceQueue<MonitoredObject> queue = new ReferenceQueue<>();
	private final ReferenceQueue<Object> objectQueue = new ReferenceQueue<>();
	private final HashMap<PhantomReference, List<Consumer>> mappings = new HashMap<>();
	private final HashMap<Class, Function<String, Object>> dumpHandlers = new HashMap<>();
	@SuppressWarnings("unchecked")
	public <T> void register(T object, Consumer<T>... consumers) {
		mappings.put(new PhantomReference<>(object, objectQueue), Arrays.asList(consumers));
	}
	public void dump() {
		Bukkit.getLogger().info("object monitor queue contents");
		for (PhantomReference ref : mappings.keySet()) {
			Bukkit.getLogger().info(" :" + ref.toString());
		}
	}
	@SuppressWarnings("unchecked")
	private Class match(Class type) {
		int depth = Integer.MAX_VALUE;
		Class[] assignable = dumpHandlers.keySet()
				.stream().filter(type::isAssignableFrom).toArray(Class[]::new);
		// TODO: finish
		return null;
	}
	// just a simple queue, not actually checked in dump()
	public void queue(MonitoredObject object) {
		new PhantomReference<>(object, queue);
	}
	@SuppressWarnings({"SuspiciousMethodCalls", "unchecked"})
	public void tick() {
		Reference<? extends MonitoredObject> ref;
		while ((ref = queue.poll()) != null) {
			MonitoredObject object = ref.get();
			if (object != null)
				object.collect();
		}
		Reference<?> objRef;
		while ((objRef = objectQueue.poll()) != null) {
			List<Consumer> consumers = mappings.remove(objRef);
			Object obj = objRef.get();
			if (consumers != null && obj != null)
				consumers.stream().forEach(consumer -> consumer.accept(obj));
		}
	}
}
