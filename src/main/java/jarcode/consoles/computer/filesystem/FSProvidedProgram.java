package jarcode.consoles.computer.filesystem;

import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.ProgramInstance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

// Program with normal Java functionality to it. Make sure these are safe!
// These are special kinds of files in the computer's filesystem, too.

// These need to be registered in the Computer class, so they can be restored later,
// and used on creation.

@SuppressWarnings("SpellCheckingInspection")
public abstract class FSProvidedProgram extends FSBlock {

	private static final byte ID = 0x02;

	protected static final Charset UTF_8 = Charset.forName("UTF-8");

	protected OutputStream out;
	protected InputStream in;

	private Computer computer;

	private ProgramInstance instance;

	public FSProvidedProgram() {
		super(ID);
	}

	@Override
	public int size() {
		return 0;
	}
	public void init(OutputStream out, InputStream in, String str,
	                 Computer computer, ProgramInstance instance) throws Exception {
		this.in = in;
		this.out = out;
		this.instance = instance;
		run(str, computer);
	}
	public abstract void run(String str, Computer computer) throws Exception;
	protected void print(String formatted) {
		try {
			out.write(formatted.getBytes(UTF_8));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected void println(String formatted) {
		print(formatted + '\n');
	}
	protected void nextln() {
		print("\n");
	}
	protected String[] splitArguments(String input) {
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
		Iterator<String> it = args.iterator();
		while (it.hasNext()) {
			if (it.next().isEmpty())
				it.remove();
		}
		return args.toArray(new String[args.size()]);
	}
	protected String[] parseFlags(String[] args, BiConsumer<Character, String> consumer, Function<Character, Boolean> hasData) {
		FlagMappings mappings = mapFlags(args);
		mappings.map.entrySet().stream().forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
		return mappings.parsed;
	}
	protected FlagMappings mapFlags(String[] args) {
		return mapFlags(args, (c) -> true);
	}
	protected FlagMappings mapFlags(String[] args, Function<Character, Boolean> hasData) {
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
	protected FSBlock resolve(String input) {
		return computer.resolve(input, this);
	}
	protected boolean terminated() {
		return instance.isTerminated();
	}
	protected String handleEncapsulation(String input) {

		if (input.equals("\""))
			return input;
		else if (input.startsWith("\"") && input.endsWith("\"")) {
			return input.substring(1, input.length() - 1);
		}
		else return input;
	}
	protected static final class FlagMappings {
		public HashMap<Character, String> map;
		public String[] parsed;
		private FlagMappings(HashMap<Character, String> map, String[] parsed) {
			this.map = map;
			this.parsed = parsed;
		}
	}
}
