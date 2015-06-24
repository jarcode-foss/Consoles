package ca.jarcode.consoles.computer;

import ca.jarcode.consoles.Consoles;
import ca.jarcode.consoles.computer.filesystem.FSFile;
import org.bukkit.Bukkit;
import org.luaj.vm2.LuaError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/*

These are a set of methods to be used for programs, be interpreted or provided. This is
a class full of methods meant to be statically imported.

 */
public class ProgramUtils {

	/**
	 * Splits the program argument string into an array of arguments. Handles string
	 * encapsulation.
	 *
	 * @param input the string to parse
	 * @return an array of arguments
	 */
	public static String[] splitArguments(String input) {
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

	/**
	 * Sleeps for the given time, halting the current thread. Throws a LuaError instead of an
	 * interrupted exception when interrupted.
	 *
	 * @param ms the time (in milliseconds) to sleep for
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e) {
			throw new LuaError(e);
		}
	}

	/**
	 * Schedules a task to be ran in the main thread and halts the Lua program until the
	 * task is complete. Used for bukkit API calls and NMS code access.
	 *
	 * @param supplier the task to run, returning a value of some sort.
	 * @param terminated a supplier to determine if the program or task should be terminated.
	 * @param <T> the type to return after the task is complete
	 * @return the result of the task
	 */
	@SuppressWarnings("unchecked")
	public static <T> T schedule(Supplier<T> supplier, BooleanSupplier terminated) {
		AtomicBoolean available = new AtomicBoolean(false);
		final Object[] result = {null};
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			result[0] = supplier.get();
			available.set(true);
		});
		try {
			while (!available.get()) {
				if (terminated.getAsBoolean())
					break;
				Thread.sleep(40);
			}
		}
		catch (InterruptedException e) {
			throw new LuaError(e);
		}
		return (T) result[0];
	}

	/**
	 * Task schedule method, used without a return type.
	 *
	 * @param task a reference to the task to execute.
	 * @param terminated whether the program or task has been terminated
	 * @see ca.jarcode.consoles.computer.ProgramUtils#schedule(java.util.function.Supplier, java.util.function.BooleanSupplier)
	 */
	public static void main(Consumer<Runnable> task, BooleanSupplier terminated) {
		AtomicBoolean resumed = new AtomicBoolean(false);
		Runnable reset = () -> resumed.set(true);
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> task.accept(reset));
		try {
			while (!resumed.get()) {
				if (terminated.getAsBoolean())
					break;
				Thread.sleep(40);
			}
		}
		catch (InterruptedException e) {
			throw new LuaError(e);
		}
	}

	/**
	 * Task schedule method, used without a return type.
	 *
	 * @param runnable the task to execute.
	 * @param terminated whether the program or task has been terminated
	 * @see ca.jarcode.consoles.computer.ProgramUtils#schedule(java.util.function.Supplier, java.util.function.BooleanSupplier)
	 */
	public static void schedule(Runnable runnable, BooleanSupplier terminated) {
		AtomicBoolean available = new AtomicBoolean(false);
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			runnable.run();
			available.set(true);
		});
		try {
			while (!available.get()) {
				if (terminated.getAsBoolean())
					break;
				Thread.sleep(40);
			}
		}
		catch (InterruptedException e) {
			throw new LuaError(e);
		}
	}

	public static String readFully(FSFile file, BooleanSupplier terminated) {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		try (InputStream is = file.createInput()) {
			int i;
			while (true) {
				if (terminated.getAsBoolean())
					break;
				if (is.available() > 0 || is instanceof ByteArrayInputStream) {
					i = is.read();
					if (i == -1) break;
					buf.write(i);
				} else Thread.sleep(50);
			}
		} catch (Exception e) {
			throw new LuaError(e);
		}

		return new String(buf.toByteArray(), StandardCharsets.UTF_8);
	}

	/**
	 * Executes the given task in the main thread.
	 *
	 * @param runnable the task to execute.
	 */
	public static void schedule(Runnable runnable) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), runnable);
	}

	/**
	 * Parses flags from an array of program arguments.
	 *
	 * @param args the arguments to parse
	 * @param consumer the handler for flags that occur in the arguments
	 * @param hasData function for whether the given flag requires extra data
	 * @return the parsed flags
	 */
	public static String[] parseFlags(String[] args, BiConsumer<Character, String> consumer, Function<Character, Boolean> hasData) {
		FlagMappings mappings = mapFlags(args, hasData);
		mappings.map.entrySet().stream().forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
		return mappings.parsed;
	}
	public static FlagMappings mapFlags(String[] args) {
		return mapFlags(args, (c) -> true);
	}
	public static FlagMappings mapFlags(String[] args, Function<Character, Boolean> hasData) {
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
		public final HashMap<Character, String> map;
		public final String[] parsed;
		private FlagMappings(HashMap<Character, String> map, String[] parsed) {
			this.map = map;
			this.parsed = parsed;
		}
	}
}
