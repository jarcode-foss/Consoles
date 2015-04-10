package jarcode.consoles.computer;

import org.bukkit.Bukkit;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_8_R2.CraftServer;
import org.bukkit.craftbukkit.v1_8_R2.command.VanillaCommandWrapper;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ComputerHandler implements Listener {

	private static ComputerHandler instance;

	// we inject a vanilla command intended to be ran by command blocks
	public static void registerLinkCommand() {
		SimpleCommandMap commandMap = ((CraftServer) Bukkit.getServer()).getCommandMap();
		commandMap.register("minecraft:", new VanillaCommandWrapper(new LinkCommand()));
	}

	public static ComputerHandler getInstance() {
		return instance;
	}

	{
		instance = this;
	}

	public ComputerHandler() {
		registerLinkCommand();
	}

	private ArrayList<Computer> computers = new ArrayList<>();
	private HashMap<String, CommandBlock> linkRequests = new HashMap<>();

	private List<Computer> getComputers(UUID uuid) {
		return computers.stream()
				.filter(computer -> computer.getOwner().equals(uuid))
				.collect(Collectors.toList());
	}
	public boolean hostnameTaken(String hostname) {
		return computers.stream().filter(comp -> comp.getHostname().equals(hostname))
				.findFirst().isPresent();
	}
	public Computer find(String hostname) {
		return computers.stream().filter(comp -> comp.getHostname().equals(hostname))
				.findFirst().orElseGet(() -> null);
	}
	public void request(String hostname, CommandBlock block) {
		linkRequests.put(hostname, block);
		find(hostname).requestDevice(block, event -> linkRequests.remove(hostname));
	}
	public void register(Computer computer) {
		computers.add(computer);
	}
	public void unregister(Computer computer) {
		computers.remove(computer);
	}
}
