package ca.jarcode.consoles.drone;

import ca.jarcode.consoles.computer.interpreter.types.LuaChest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public interface Drone {
	// all of these methods are allowed to block and are called from Lua program threads
	// TODO: Implement a drone. This abstraction is here because I don't know what to make a drone with atm.
	void move(BlockFace face);
	Location getLocation();
	void remove();
	void mine(BlockFace face);
	void use(BlockFace face);
	Material getType(BlockFace face);
	LuaChest open(BlockFace face);
	LuaChest inventory();
}
