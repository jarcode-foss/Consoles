package jarcode.consoles.computer.boot;

import com.google.common.collect.HashBiMap;
import jarcode.consoles.Consoles;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.Terminal;
import jarcode.consoles.computer.bin.CurrentDirectoryProgram;
import jarcode.consoles.computer.bin.ShowDirectoryProgram;
import jarcode.consoles.computer.filesystem.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.io.FileNotFoundException;
import java.io.IOException;
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

	{
		programs.put((byte) 0x00, this);
		programs.put((byte) 0x01, new CurrentDirectoryProgram());
		programs.put((byte) 0x02, new ShowDirectoryProgram());
	}

	{
		driverMappings.put("cmd", CommandBlockDriver.class);
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
				root.contents.put("home", home);
				root.contents.put("bin", bin);
				root.contents.put("dev", new FSFolder());
				root.contents.put("tmp", new FSFolder());
				root.contents.put("X11", x11);
				home.contents.put("admin", new FSFolder());
				FSStoredFile file = new FSStoredFile();
				try {
					file.createOutput().write("One can only dream".getBytes(Charset.forName("UTF-8")));
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				x11.contents.put("xorg.conf", file);
				systemPath.add("bin");
				mapProgram((byte) 0x01, root, "cd");
				mapProgram((byte) 0x02, root, "dir", "ls");
			}
			private void mapProgram(byte id, FSFolder root, String... names) {
				try {
					FSFolder bin = (FSFolder) root.get("bin");
					for (String name : names) {
						bin.contents.put(name, getProgram(id));
					}
				}
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		});
		activities.put("boot", new FSProvidedProgram() {
			@Override
			public void run(String str, Computer computer) throws Exception {

			}
		});
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
		try {
			FSBlock block = computer.getRoot().get(path);
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
		catch (FileNotFoundException e) {
			return -1;
		}
	}
	// drivers!
	public void tick() {
		if (!missingDevFolderError) try {
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
							}
							catch (InstantiationException | NoSuchMethodException
									| IllegalAccessException | InvocationTargetException e) {
								Terminal terminal = computer.getCurrentTerminal();
								if (terminal != null) {
									terminal.advanceLine();
									terminal.println(ChatColor.RED + "KERNEL: failed to load driver for /dev/"
											+ name + ChatColor.WHITE + " (" + e.getClass().getSimpleName() + ")");
									terminal.println(ChatColor.RED + "KERNEL: Uninstalling driver type.");
									if (Consoles.DEBUG)
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
		drivers.forEach(Driver::tick);
	}
}
