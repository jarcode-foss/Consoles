package jarcode.consoles.computer;

import net.minecraft.server.v1_8_R2.TileEntityCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_8_R2.CraftServer;
import org.bukkit.craftbukkit.v1_8_R2.command.VanillaCommandWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ComputerHandler {

	private HashMap<String, TileEntityCommand> linkRequests = new HashMap<>();

	public static void registerLinkCommand() {
		SimpleCommandMap commandMap = ((CraftServer) Bukkit.getServer()).getCommandMap();
		commandMap.register("minecraft:", new VanillaCommandWrapper(new LinkCommand()));
	}

	private ArrayList<Computer> computers = new ArrayList<>();

	private List<Computer> getComputers(UUID uuid) {
		return computers.stream()
				.filter(computer -> computer.getOwner().equals(uuid))
				.collect(Collectors.toList());
	}
	public void register(Computer computer) {
		computers.add(computer);
	}
}
