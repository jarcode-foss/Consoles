package jarcode.consoles.computer;

import jarcode.consoles.*;
import jarcode.consoles.computer.boot.Kernel;
import jarcode.consoles.computer.devices.CommandDevice;
import jarcode.consoles.computer.filesystem.FSBlock;
import jarcode.consoles.computer.filesystem.FSFile;
import jarcode.consoles.computer.filesystem.FSFolder;
import jarcode.consoles.computer.filesystem.FSProvidedProgram;
import jarcode.consoles.event.ButtonEvent;
import jarcode.consoles.event.ConsoleEventListener;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R2.ChatComponentText;
import net.minecraft.server.v1_8_R2.TileEntityCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_8_R2.block.CraftCommandBlock;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Computer implements Runnable {

	public static final String VERSION = "1.19.2";

	static final Position2D STATUS_COMPONENT_POSITION = new Position2D(2, 2);
	static final Position2D ROOT_COMPONENT_POSITION = new Position2D(2, 4 + StatusBar.HEIGHT);

	private String hostname;

	private ManagedConsole console;

	private FSFolder root = new FSFolder();

	private ConsoleComponent[] feeds = new ConsoleComponent[8];

	private int componentIndex = -1;

	private UUID owner;

	private Kernel kernel;

	private int taskId;

	private StatusBar bar;

	public Computer(String hostname, UUID owner, int width, int height) {
		this.hostname = hostname;
		this.owner = owner;
		console = new ManagedConsole(width, height);
		feeds[0] = new Terminal(this, false, ROOT_COMPONENT_POSITION);
		setScreenIndex(0);
	}

	public void setRoot(FSFolder root) {
		this.root = root;
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
	public void status(String status) {
		bar.setText(status);
		console.repaint();
	}
	// Creates and installs the computer.
	public void create(BlockFace face, Location location) {
		console.create(face, location);
		bar = new StatusBar(console);
		console.putComponent(STATUS_COMPONENT_POSITION, bar);
		getCurrentTerminal().println(ChatColor.GREEN + "Network boot: " + ChatColor.WHITE + "(" + hostname + ")");
		getCurrentTerminal().advanceLine();
		console.repaint();
		printAfter("Loading vmlinuz", 20);
		for (int t = 0; t < 3; t++) {
			printAfter(".", 28 + (t * 8));
			if (t == 2)
				printAfter("\n", 29 + (t * 8));
		}
		printAfter("Loading initrd.gz", 50);
		for (int t = 0; t < 3; t++) {
			printAfter(".", 58 + (t * 8));
			if (t == 2)
				printAfter("\n", 59 + (t * 8));
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			if (!root.exists("boot/vmlinuz")) {
				try {
					kernel = Kernel.install(Computer.this);
					kernel.routine("install");
				} catch (Exception e) {
					printAfter("Failed to install kernel: " + e.getClass(), 2);
					e.printStackTrace();
					return;
				}
			}
			else {
				kernel = boot("boot/vmlinuz", Kernel.class);
			}
			kernel.routine("boot");
			// register main task
			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Consoles.getInstance(), Computer.this, 1L, 1L);
			getCurrentTerminal().clear();
			getCurrentTerminal().onStart();
			console.repaint();
		}, 80L);
	}
	private void printAfter(final String text, long delay) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			getCurrentTerminal().print(text);
			console.repaint();
		}, delay);
	}
	public boolean setScreenIndex(int index) {
		if (index >= 8 || index < 0)
			throw new ArrayIndexOutOfBoundsException();
		console.putComponent(ROOT_COMPONENT_POSITION, feeds[index]);
		componentIndex = index;
		if (console.created())
			console.repaint();
		return true;
	}
	public void requestDevice(CommandBlock block, ConsoleEventListener<ConsoleButton, ButtonEvent> listener) {
		TileEntityCommand command = ((CraftCommandBlock) block).getTileEntity();
		ConsoleButton allow = new ConsoleButton(console, "Allow");
		ConsoleButton deny = new ConsoleButton(console, "Deny");
		Location loc = block.getLocation();
		Position2D pos = dialog(String.format("Command block at (%s,%d,%d,%d) wants to connect to this console",
				loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())
				, allow, deny);
		allow.addEventListener(event -> {
			command.getCommandBlock().sendMessage(new ChatComponentText("Connection accepted"));
			addDeviceFile("cmd", new CommandDevice(block));
			console.removeComponent(pos);
		});
		deny.addEventListener(event -> {
			command.getCommandBlock().sendMessage(new ChatComponentText("Computer denied connection"));
			console.removeComponent(pos);
		});
		if (listener != null)
			allow.addEventListener(listener);
	}
	public Position2D dialog(String text, ConsoleComponent... components) {
		Map.Entry<Position2D, ConsoleDialog> entry = ConsoleDialog.create(console, text, components);
		Position2D pos = entry.getKey();
		while (console.componentAt(pos))
			pos = new Position2D(pos.getX() + 3, pos.getY() + 3);
		if (pos.getX() > 0 && pos.getY() > 0) {
			console.putComponent(pos, entry.getValue());
			console.repaint();
			return pos;
		}
		else return null;
	}
	public void addDeviceFile(String baseName, FSFile file) {
		try {
			FSFolder folder = (FSFolder) getRoot().get("dev");
			int index = 0;
			while (folder.contents.keySet().contains(baseName + index))
				index++;
			folder.contents.put(baseName + index, file);
		}
		// ignore adding device files when the device folder doesn't exist
		catch (FileNotFoundException | ClassCastException ignored) {}
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
	public ManagedConsole getConsole() {
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
	// used by programs
	public boolean switchView(int view) {
		view--;
		if (view < 0 || view >= feeds.length)
			return false;
		int v = view;
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			setScreenIndex(v);
			status("switched to session: " + (v + 1));
		});
		return true;
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
	public FSBlock resolve(String input, Object program) {
		return getBlock(input, getTerminal(program).getCurrentDirectory());
	}
	public FSBlock getBlock(String input, String currentDirectory) {
		// start from root
		if (input.startsWith("/")) {
			input = input.substring(1);
			if (input.isEmpty())
				return root;
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
				String str;
				if (currentDirectory.startsWith("/"))
					str = currentDirectory.substring(1);
				else str = currentDirectory;
				FSBlock block = root.get(str);
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
