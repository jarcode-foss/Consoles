package jarcode.consoles.computer;

import jarcode.consoles.Consoles;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/*

These are a set of methods to be used for programs, be interpreted or provided. This is
an interface full of default methods (instead of an abstract class), because FSProvidedProgram
needs to inherit from FSBlock.

 */
public interface Program {

	public default String[] splitArguments(String input) {
		char[] arr = input.toCharArray();
		boolean quote = false;
		List<String> args = new ArrayList<>();
		StringBuilder arg = new StringBuilder();
		for (int t = 0; t < arr.length; t++) {
			if (!quote) {
				if (arr[t] == ' ') {
					args.add(arg.toString());
					arg = new StringBuilder();
				}
				// start encapsulation on only the first character of an argument
				else if (arr[t] == '\"' && arg.length() == 0) {
					quote = true;
				}
				else {
					arg.append(arr[t]);
				}
			}
			else {
				// end encapsulation if quote isn't preceded by \
				if (arr[t] == '\"' && (t == 0 || arr[t - 1] != '\\')) {
					quote = false;
					args.add(arg.toString());
					arg = new StringBuilder();
				}
				// ignore \" sequence
				else if (arr[t] != '\\' || t == arr.length - 1 || arr[t + 1] == '\"'){
					arg.append(arr[t]);
				}
			}
		}
		if (arg.length() > 0)
			args.add(arg.toString());
		Iterator<String> it = args.iterator();
		while (it.hasNext()) {
			if (it.next().isEmpty())
				it.remove();
		}
		return args.toArray(new String[args.size()]);
	}
	// huehuehuehuehuehue
	@SuppressWarnings("unchecked")
	public default <T> T schedule(Supplier<T> supplier, BooleanSupplier terminated) throws InterruptedException {
		AtomicBoolean available = new AtomicBoolean(false);
		final Object[] result = {null};
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			result[0] = supplier.get();
			available.set(true);
		});
		while (!available.get()) {
			if (terminated.getAsBoolean())
				break;
			Thread.sleep(40);
		}
		return (T) result[0];
	}
	public default void main(Consumer<Runnable> task, BooleanSupplier terminated) throws InterruptedException {
		AtomicBoolean resumed = new AtomicBoolean(false);
		Runnable reset = () -> resumed.set(true);
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> task.accept(reset));
		while (!resumed.get()) {
			if (terminated.getAsBoolean())
				break;
			Thread.sleep(40);
		}
	}
	public default void schedule(Runnable runnable, BooleanSupplier terminated) throws InterruptedException {
		AtomicBoolean available = new AtomicBoolean(false);
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			runnable.run();
			available.set(true);
		});
		while (!available.get()) {
			if (terminated.getAsBoolean())
				break;
			Thread.sleep(40);
		}
	}
	public default void schedule(Runnable runnable) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), runnable);
	}
	public default String[] parseFlags(String[] args, BiConsumer<Character, String> consumer, Function<Character, Boolean> hasData) {
		FlagMappings mappings = mapFlags(args, hasData);
		mappings.map.entrySet().stream().forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
		return mappings.parsed;
	}
	public default FlagMappings mapFlags(String[] args) {
		return mapFlags(args, (c) -> true);
	}
	public default FlagMappings mapFlags(String[] args, Function<Character, Boolean> hasData) {
		HashMap<Character, String> map = new HashMap<>();
		List<String> parsed = new ArrayList<>();
		Character flag = null;
		boolean quote = false;
		StringBuilder builder = new StringBuilder();
		for (String arg : args) {
			if (flag != null) {
				if (!quote && arg.startsWith("\"")) {
					arg = arg.substring(1);
					quote = true;
				}
				if (quote) {
					char[] arr = arg.toCharArray();
					for (int t = 0; t < arr.length; t++) {
						// end encapsulation if quote isn't preceded by \
						if (arr[t] == '\"' && (t == 0 || arr[t - 1] != '\\')) {
							quote = false;
							map.put(flag, builder.toString());
							builder = new StringBuilder();
							flag = null;
						}
						// ignore the slash in the \" sequence
						else if (arr[t] != '\\' || t == arr.length - 1 || arr[t + 1] == '\"'){
							builder.append(arr[t]);
						}
					}
				}
				else {
					map.put(flag, arg);
					flag = null;
				}
			}
			else if (arg.startsWith("-") && arg.length() > 1) {
				flag = arg.charAt(1);
				if (!hasData.apply(flag)) {
					map.put(flag, null);
					flag = null;
					// this allows using things like -rf (combined args)
					if (arg.length() > 2) for (char c : arg.substring(2).toCharArray()) {
						if (!hasData.apply(c))
							map.put(c, null);
					}
				}
				else if (arg.length() > 2) {
					if (arg.charAt(2) == '"') {
						quote = true;
						if (arg.length() > 3)
							builder.append(arg.substring(3));
					}
					else {
						map.put(flag, arg.substring(2));
						flag = null;
					}
				}
			}
			else parsed.add(arg);
		}
		return new FlagMappings(map, parsed.toArray(new String[parsed.size()]));
	}
	public static final class FlagMappings {
		public HashMap<Character, String> map;
		public String[] parsed;
		private FlagMappings(HashMap<Character, String> map, String[] parsed) {
			this.map = map;
			this.parsed = parsed;
		}
	}
}
