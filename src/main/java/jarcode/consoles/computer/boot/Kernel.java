package jarcode.consoles.computer.boot;

import com.google.common.collect.HashBiMap;
import jarcode.consoles.Consoles;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.bin.*;
import jarcode.consoles.computer.devices.NullDevice;
import jarcode.consoles.computer.devices.PlayerCommandDevice;
import jarcode.consoles.computer.devices.PlayerInteractDevice;
import jarcode.consoles.computer.filesystem.*;
import jarcode.consoles.computer.interpreter.LuaDefaults;
import org.bukkit.ChatColor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.*;

// fake operating system kernel, but contains important activities and driver management.
public class Kernel extends FSProvidedProgram {

	private final HashBiMap<Byte, FSProvidedProgram> programs = HashBiMap.create();
	private HashMap<String, FSProvidedProgram> activities = new HashMap<>();
	private List<String> systemPath = new ArrayList<>();
	private List<Driver> drivers = new ArrayList<>();
	private Map<String, Class<? extends Driver>> driverMappings = new HashMap<>();
	private Computer computer;
	private boolean missingDevFolderError = false;

	private int driverTick = 0;

	{
		program(0x00, this);
		program(0x01, new CurrentDirectoryProgram());
		program(0x02, new ShowDirectoryProgram());
		program(0x03, new WriteProgram());
		program(0x04, new CatProgram());
		program(0x05, new DeleteProgram());
		program(0x06, new HostnameProgram());
		program(0x07, new ClearProgram());
		program(0x08, new FlashProgram());
		program(0x09, new ViewProgram());
		program(0x0A, new EditProgram());
		program(0x0B, new TouchProgram());
		program(0x0C, new RemoveProgram());
		program(0x0D, new HelpProgram());
		program(0x0E, new MakeDirectoryProgram());
		program(0x0F, new OwnerProgram());
		program(0x10, new ManualProgram());
		program(0x11, new JokeProgram());
		program(0x12, new CopyProgram());
		program(0x13, new WGetProgram());
		program(0x14, new MapProgram());
		program(0x15, new SkriptProgram());
		program(0x16, new ExecuteProgram());
	}

	{
		driverMappings.put("cmd", CommandBlockDriver.class);
		driverMappings.put("pcmd", PlayerCommandDriver.class);
	}

	public static Kernel install(Computer computer) throws Exception {
		Kernel kernel = new Kernel();
		FSFolder boot = new FSFolder();
		boot.contents.put("vmlinuz", kernel);
		computer.getRoot().contents.put("boot", boot);
		kernel.run("", computer);
		return kernel;
	}

	{
		activities.put("install", new FSProvidedProgram() {
			@Override
			public void run(String str, Computer computer) throws Exception {
				Consoles.getInstance().getLogger().info("Flashing new computer: " + computer.getHostname()
						+ ", owner:" + computer.getOwner());
				FSFolder root = computer.getRoot();

				FSFolder home = new FSFolder();
				FSFolder bin = new FSFolder();
				FSFolder x11 = new FSFolder();
				FSFolder dev = new FSFolder();
				root.contents.put("home", home);
				root.contents.put("bin", bin);
				root.contents.put("dev", dev);
				root.contents.put("tmp", new FSFolder());
				root.contents.put("X11", x11);
				home.contents.put("admin", new FSFolder());
				FSStoredFile file = writtenFile("One can only dream.");
				x11.contents.put("xorg.conf", file);
				flashPrograms();
			}
		});
		activities.put("boot", new FSProvidedProgram() {
			@Override
			public void run(String str, Computer computer) throws Exception {
				systemPath.add("bin");
				FSBlock block = computer.getBlock("/dev", "/");
				if (block instanceof FSFolder) {
					((FSFolder) block).contents.put("null", new NullDevice());
					((FSFolder) block).contents.put("pcmd0", new PlayerCommandDevice(computer));
					((FSFolder) block).contents.put("pint0", new PlayerInteractDevice(computer));
				}
			}
		});
	}
	public void flashPrograms() {
		FSFolder root = computer.getRoot();
		mapProgram(0x01, root, "cd");
		mapProgram(0x02, root, "dir", "ls");
		mapProgram(0x03, root, "write");
		mapProgram(0x04, root, "cat");
		mapProgram(0x05, root, "delete");
		mapProgram(0x06, root, "hostname");
		mapProgram(0x07, root, "clear");
		mapProgram(0x08, root, "flash");
		mapProgram(0x09, root, "view");
		mapProgram(0x0A, root, "edit", "emacs");
		mapProgram(0x0B, root, "touch");
		mapProgram(0x0C, root, "rm");
		mapProgram(0x0D, root, "help");
		mapProgram(0x0E, root, "mkdir");
		mapProgram(0x0F, root, "owner");
		mapProgram(0x10, root, "man");
		mapProgram(0x11, root, "joke");
		mapProgram(0x12, root, "cp");
		mapProgram(0x13, root, "wget");
		mapProgram(0x14, root, "map");
		mapProgram(0x15, root, "skript");
		mapProgram(0x16, root, "exec");

		LuaDefaults.loadInto(computer);
	}
	public static FSStoredFile writtenFile(String text) {
		FSStoredFile file = new FSStoredFile();
		try {
			OutputStream out = file.createOutput();
			out.write(text.getBytes(Charset.forName("UTF-8")));
			out.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}
	private void mapProgram(int id, FSFolder root, String... names) {
		try {
			if (!root.exists("bin"))
				root.contents.put("bin", new FSFolder());
			FSFolder bin = (FSFolder) root.get("bin");
			for (String name : names) {
				bin.contents.put(name, getProgram((byte) id));
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	public HashBiMap<Byte, FSProvidedProgram> providedPrograms() {
		HashBiMap<Byte, FSProvidedProgram> map = HashBiMap.create();
		programs.entrySet().stream()
				.forEach(en -> map.put(en.getKey(), en.getValue()));
		return map;
	}
	private void program(int id, FSProvidedProgram providedProgram) {
		programs.put((byte) id, providedProgram);
	}
	public List<String> getSystemPath() {
		return systemPath;
	}
	public FSProvidedProgram getProgram(byte id) {
		return programs.get(id);
	}
	public byte getId(FSProvidedProgram program) {
		return programs.inverse().get(program);
	}
	@Override
	public void run(String str, Computer computer) throws Exception {
		this.computer = computer;
	}
	public void routine(String name) {
		try {
			FSProvidedProgram program = activities.get(name);
			if (program != null) {
				program.run(name, computer);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void registerDriver(Driver driver) {
		drivers.add(driver);
	}
	public int stopDriversForDevice(String path) {
		FSBlock block = computer.resolve(path, "/");
		if (block instanceof FSFile) {
			Iterator<Driver> it = drivers.iterator();
			int count = 0;
			while (it.hasNext()) {
				Driver driver = it.next();
				if (driver.getDevice() == block) {
					driver.stop();
					it.remove();
					count++;
				}
			}
			return count;
		}
		else return -1;
	}
	// drivers!
	public void tick() {
		if (!missingDevFolderError && driverTick % 10 == 0) try {
			driverTick = 0;
			FSBlock devices = computer.getRoot().get("dev");
			if (!(devices instanceof FSFolder)) {
				if (!missingDevFolderError) {
					Terminal terminal = computer.getCurrentTerminal();
					if (terminal != null) {
						terminal.println(ChatColor.RED + "KERNEL: /dev is not a folder! Aborting device search routine.");
						missingDevFolderError = true;
					}
				}
			}
			else for (Map.Entry<String, FSBlock> entry : ((FSFolder) devices).contents.entrySet()) {
				if (entry.getValue() instanceof FSFile) {
					FSFile file = (FSFile) entry.getValue();
					boolean mounted = false;
					for (Driver driver : drivers) {
						if (file == driver.getDevice()) {
							mounted = true;
							break;
						}
					}
					if (!mounted) {
						String match = null;
						String name = null;
						for (String key : driverMappings.keySet()) {
							if (entry.getKey().startsWith(key)) {
								match = key;
								name = entry.getKey();
								break;
							}
						}
						if (match != null) {
							Class<? extends Driver> type = driverMappings.get(match);
							try {
								Driver driver = type.getConstructor(FSFile.class, Computer.class)
										.newInstance(file, computer);
								registerDriver(driver);
								computer.status(ChatColor.RED + "KERNEL: " + ChatColor.WHITE + "loaded driver for /dev/" + name);
							}
							catch (InstantiationException | NoSuchMethodException
									| IllegalAccessException | InvocationTargetException e) {
								Terminal terminal = computer.getCurrentTerminal();
								if (terminal != null) {
									terminal.advanceLine();
									terminal.println(ChatColor.RED + "KERNEL: failed to load driver for /dev/"
											+ name + ChatColor.WHITE + " (" + e.getClass().getSimpleName() + ")");
									terminal.println(ChatColor.RED + "KERNEL: Uninstalling driver type.");
									if (Consoles.debug)
										e.printStackTrace();
									driverMappings.remove(match);
								}
							}
						}
					}
				}
			}
		}
		catch (FileNotFoundException e) {
			if (!missingDevFolderError) {
				Terminal terminal = computer.getCurrentTerminal();
				if (terminal != null) {
					terminal.advanceLine();
					terminal.println(ChatColor.RED + "KERNEL: /dev folder missing when " +
							"listening for devices to install");
					terminal.println(ChatColor.RED + "KERNEL: Aborting device search routine.");
					missingDevFolderError = true;
				}
			}
		}
		driverTick++;
		drivers.forEach(Driver::tick);
	}
}
