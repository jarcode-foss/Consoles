package jarcode.consoles.computer;

import jarcode.consoles.*;
import jarcode.consoles.api.*;
import jarcode.consoles.computer.boot.Kernel;
import jarcode.consoles.computer.devices.CommandDevice;
import jarcode.consoles.computer.filesystem.*;
import jarcode.consoles.computer.interpreter.Lua;
import jarcode.consoles.event.ButtonEvent;
import jarcode.consoles.event.ConsoleEventListener;
import net.minecraft.server.v1_8_R2.*;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_8_R2.block.CraftCommandBlock;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class Computer implements Runnable {

	// Lua<->Java mappings
	static {
		Lua.map(Computer::lua_session, "switchSession");
		Lua.map(Computer::lua_dialog, "dialog");
		Lua.map(Computer::lua_messageOwner, "tellOwner");
	}

	public static final String VERSION = "1.19.2";

	static final Position2D STATUS_COMPONENT_POSITION = new Position2D(2, 2);
	public static final Position2D ROOT_COMPONENT_POSITION = new Position2D(2, 2 + StatusBar.HEIGHT);

	private String hostname;

	private ManagedConsole console;

	private FSFolder root = new FSFolder();

	private ConsoleComponent[] feeds = new ConsoleComponent[8];

	private int componentIndex = -1;

	private UUID owner;

	private Kernel kernel;

	private int taskId;

	private StatusBar bar;

	private List<BiConsumer<String, String>> listeners = new CopyOnWriteArrayList<>();
	private List<BiConsumer<String, Position2D>> interactListeners = new CopyOnWriteArrayList<>();

	private Map<String, Consumer<String>> messageListeners = new ConcurrentHashMap<>();

	public Computer(String hostname, UUID owner, ManagedConsole console) {
		this.hostname = hostname;
		this.owner = owner;
		this.console = console;
		feeds[0] = new Terminal(this, false);
		setScreenIndex(0);
	}

	public Computer(String hostname, UUID owner, int width, int height) {
		this.hostname = hostname;
		this.owner = owner;
		console = new ManagedConsole(width, height);
		feeds[0] = new Terminal(this, false);
		setScreenIndex(0);
	}

	public boolean hostname(String hostname) {
		this.hostname = hostname;
		for (ConsoleComponent component : feeds) {
			if (component instanceof Terminal) {
				((Terminal) component).updatePrompt();
			}
		}
		return true;
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
	public void unregisterClickListener(BiConsumer<String, Position2D> consumer) {
		interactListeners.remove(consumer);
	}
	public void registerClickListener(BiConsumer<String, Position2D> consumer) {
		interactListeners.add(consumer);
	}
	public void unregisterCommandListener(BiConsumer<String, String> consumer) {
		listeners.remove(consumer);
	}
	public void registerCommandListener(BiConsumer<String, String> consumer) {
		listeners.add(consumer);
	}
	public boolean isChannelRegistered(String channel) {
		return messageListeners.containsKey(channel);
	}
	public void registerMessageListener(String channel, Consumer<String> consumer) {
		messageListeners.put(channel, consumer);
	}
	public void unregisterMessageListener(String channel) {
		messageListeners.remove(channel);
	}
	public Consumer<String> getMessageListener(String channel) {
		return messageListeners.get(channel);
	}
	public void clickEvent(Position2D pos, String player) {
		interactListeners.stream().forEach(consumer -> consumer.accept(player, pos.copy()));
	}
	public void playerCommand(String command, String player) {
		listeners.stream().forEach(consumer -> consumer.accept(player, command));
	}
	public void status(String status) {
		bar.setText(status);
		console.repaint();
	}
	public int getViewWidth() {
		return getConsole().getWidth() - (2 + ROOT_COMPONENT_POSITION.getX());
	}
	public int getViewHeight() {
		return getConsole().getHeight() - (2 + ROOT_COMPONENT_POSITION.getY());
	}
	public void load(File file) throws IOException {
		try {
			// create temp kernel instance for loading fs
			kernel = Kernel.install(Computer.this);
			// create instance
			SerializedFilesystem fs = new SerializedFilesystem(this);
			// map out serialized file tree
			fs.readFrom(new FileInputStream(file));
			// call block serializers
			root = (FSFolder) fs.deserialize();
		} catch (Exception e) {
			if (e instanceof IOException)
				throw (IOException) e;
			else
				e.printStackTrace();
		}
	}
	// Creates and installs the computer.
	public void create(BlockFace face, Location location) {
		console.create(face, location);
		bar = new StatusBar(console);
		console.putComponent(STATUS_COMPONENT_POSITION, bar);
		getCurrentTerminal().println(ChatColor.GREEN + "Network boot: " + ChatColor.WHITE + "(" + hostname + ")");
		getCurrentTerminal().advanceLine();
		ComputerHandler.getInstance().updateBlocks(this);
		console.repaint();
		String[] text = {
				"Loading vmlinuz", null, ".", null, ".", null, ".", null, ".", null, ".\tdone\n",
				"Loading initrd.gz", null, ".", null, ".", null, ".", null, ".", null, "\tdone\n\n",
				"Initializing filesystem", ".", ".", ".", "\tdone\n",
				"Scanning devices", ".", ".", "\n\t[+] loaded /dev/pint0", null, "\n\t[+] loaded /dev/pcmd0\n",
				"Blacklisting kernel modules", ".", ".", "\tdone\n",
				"Starting services", null, ".", null, ".", null, ".", null, "\tdone\n",
				"Loading drivers", "\n\t[+] plcommand-1.34-245", "\n\t[+] plaction-1.06-083",
				"\n\t[+] command-1.34-174", "\n\t[+] ttyinput-1.94-042", "\n\t[+] luabindings-2.3-009"
		};
		int i = 20;
		for (String str : text) {
			if (str != null) {
				printAfter(str, i);
			}
			i += 4;
		}
		bootTask(i + 10);
	}
	private void bootTask(long time) {
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
		}, time);
	}
	public void save() {
		try {
			new ComputerData(this).save();
		} catch (IOException e) {
			Consoles.getInstance().getLogger().severe("Failed to save computer!");
			e.printStackTrace();
		}
	}
	private void printAfter(final String text, long delay) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			getCurrentTerminal().print(text);
			console.repaint();
		}, delay);
	}
	public boolean screenAvailable(int index) {
		return !(index >= 8 || index < 0) && feeds[index] == null;
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
	public void registerPainter(Integer index, CanvasPainter painter, CanvasInteractListener listener, Integer bg) {
		if (index >= 8 || index < 0)
			throw new ArrayIndexOutOfBoundsException();
		Console api = Console.wrap(console);
		CanvasComponent comp = api.newComponent(getViewWidth(), getViewHeight())
				.painter(painter)
				.listen(listener)
				.enabledHandler(() -> true, (b) -> {})
				.background((byte) (int) bg)
				.create();
		setComponent(index, (ConsoleComponent) comp);
	}
	private static void lua_session(Integer id) {
		Computer computer = Lua.context();
		computer.switchView(id);
		try {
			Thread.sleep(50);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
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

	// ALL cleanup should be done in here
	public void destroy() {
		console.remove();
		Bukkit.getScheduler().cancelTask(taskId);
		ComputerHandler.getInstance().unregister(this);
		console.getLocation().getWorld().dropItemNaturally(console.getLocation(),  ComputerHandler.newComputerStack());
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
	public int getComponentIndex() {
		return componentIndex;
	}
	public ConsoleComponent getCurrentComponent() {
		return feeds[componentIndex];
	}
	public static void lua_dialog(String text) {
		Computer computer = Lua.context();
		Lua.main(() -> computer.showDialog(text));
	}
	public static void lua_messageOwner(String text) {
		Computer computer = Lua.context();
		Lua.main(() -> {
			UUID uuid = computer.getOwner();
			Player player = Bukkit.getPlayer(uuid);
			player.sendMessage(text);
		});
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
