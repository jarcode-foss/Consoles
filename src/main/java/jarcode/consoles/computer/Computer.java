package jarcode.consoles.computer;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.UUID;

public class Computer implements Runnable {

	public static final String VERSION = "1.19.2";

	private static final Position2D ROOT_COMPONENT_POSITION = new Position2D(2, 2);

	private String hostname;

	private Console console;

	private FSFolder root = new FSFolder();

	private ConsoleComponent[] feeds = new ConsoleComponent[8];

	private int componentIndex = -1;

	private UUID owner;

	private Kernel kernel;

	private int taskId;

	public Computer(String hostname, UUID owner, int width, int height) {
		this.hostname = hostname;
		this.owner = owner;
		console = new Console(width, height);
		feeds[0] = new Terminal(this);
		setScreenIndex(0);
	}

	// This is used to boot a provided program, and actually obtain the instance of the program itself,
	// instead of the wrapper class. This will not work for Lua programs, and these are ran in the current thread.
	// basically, it's our mini boot loader
	@SuppressWarnings("unchecked")
	private  <T extends FSProvidedProgram> T boot(String path, Class<T> type) {
		try {
			FSBlock block = root.get(path);
			if (block instanceof FSProvidedProgram) {
				ProgramInstance instance = new ProgramInstance((FSProvidedProgram) block, "", this);
				if (type.isInstance(instance.provided)) {
					instance.run();
					return (T) instance.provided;
				}
			}
		}
		// we can ignore this, because the only possible cause is the file itself missing from the drive
		catch (FileNotFoundException ignored) {}
		// so, just return null if it's an invalid boot or the expected type is wrong
		return null;
	}
	// Creates and installs the computer.
	public void create(BlockFace face, Location location) {
		console.create(face, location);
		getCurrentTerminal().println(ChatColor.GREEN + "Network boot: " + ChatColor.WHITE + "(" + hostname + ")");
		getCurrentTerminal().advanceLine();
		console.repaint();
		printAfter("Loading vmlinuz", 20);
		for (int t = 0; t < 3; t++) {
			printAfter(".", 28 + (t * 8));
		}
		printAfter("Loading initrd.gz", 50);
		for (int t = 0; t < 3; t++) {
			printAfter(".", 58 + (t * 8));
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(PluginController.getInstance(), () -> {
			kernel = boot("boot/vmlinuz", Kernel.class);
			if (!root.exists("boot/vmlinuz")) {
				Kernel.install(Computer.this);
				kernel.routine("install");
			}
			kernel.routine("boot");
			// register main task
			taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(PluginController.getInstance(), Computer.this);
			getCurrentTerminal().clear();
			getCurrentTerminal().onStart();
			console.repaint();
		}, 80L);
	}
	private void printAfter(final String text, long delay) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(PluginController.getInstance(), () -> {
			getCurrentTerminal().print(text);
			console.repaint();
		}, delay);
	}
	public boolean setScreenIndex(int index) {
		if (index >= 8 || index < 0)
			throw new ArrayIndexOutOfBoundsException();
		if (feeds[index] == null)
			return false;
		console.putComponent(ROOT_COMPONENT_POSITION, feeds[index]);
		componentIndex = index;
		if (console.created())
			console.repaint();
		return true;
	}
	// This is called destroy for a reason. Because the data is stored elsewhere (not in the flimsy console/monitor),
	// you can destroy the computer and recreate it with the same hostname to get everything back. However, the computer
	// does actually stop running, it's just not gone.

	// ALL cleanup should be done in here
	public void destroy() {
		console.remove();
		Bukkit.getScheduler().cancelTask(taskId);
	}
	public Kernel getKernel() {
		return kernel;
	}
	public FSFolder getRoot() {
		return root;
	}
	public String getHostname() {
		return hostname;
	}
	public Console getConsole() {
		return console;
	}
	public UUID getOwner() {
		return owner;
	}
	public List<String> getSystemPath() {
		return kernel.getSystemPath();
	}
	public void setComponent(int index, ConsoleComponent component) {
		if (index >= 8 || index < 0)
			throw new ArrayIndexOutOfBoundsException();
		if (index == 0)
			throw new IllegalArgumentException("Cannot change default component!");
		feeds[index] = component;
		if (index == componentIndex) {
			console.putComponent(ROOT_COMPONENT_POSITION, feeds[index]);
			if (console.created())
				console.repaint();
		}
	}
	public ConsoleComponent getCurrentComponent() {
		return feeds[componentIndex];
	}
	public ConsoleDialog showDialog(String text, ConsoleComponent... children) {
		ConsoleDialog dialog = ConsoleDialog.show(console, text, children);
		console.repaint();
		return dialog;
	}
	public void showDialog(String text) {
		ConsoleButton button = new ConsoleButton(console, "Ok");
		final ConsoleDialog dialog = ConsoleDialog.show(console, text, button);
		button.addEventListener(event -> console.removeComponent(dialog));
		console.repaint();
	}
	public Terminal getCurrentTerminal() {
		return getCurrentComponent() instanceof Terminal ? (Terminal) getCurrentComponent() : null;
	}
	public void showDialogWithClose(String text, ConsoleComponent... children) {
		ConsoleButton button = new ConsoleButton(console, "Close");
		ConsoleComponent[] arr = new ConsoleComponent[children.length + 1];
		arr[0] = button;
		System.arraycopy(children, 0, arr, 1, children.length);
		final ConsoleDialog dialog = ConsoleDialog.show(console, text, arr);
		button.addEventListener(event -> console.removeComponent(dialog));
		console.repaint();
	}
	// Called by applications to get its own terminal instance
	public Terminal getTerminal(Object program) {
		for (ConsoleComponent component : feeds) {
			if (component instanceof Terminal && ((Terminal) component).getLastProgramInstance().contains(program))
				return (Terminal) component;
		}
		return null; // this is bad. If this happens, a program has been detached from its terminal instance!
	}
	public FSBlock getBlock(String input, String currentDirectory) {
		// start from root
		if (input.startsWith("/")) {
			input = input.substring(1);
			try {
				return root.get(input);
			}
			catch (FileNotFoundException e) {
				return null;
			}
		}
		// start from current directory
		else {
			try {
				FSBlock block = root.get(currentDirectory);
				return ((FSFolder) block).get(input);
			}
			catch (FileNotFoundException e) {
				return null;
			}
		}

	}
	public void run() {
		kernel.tick();
	}
}
