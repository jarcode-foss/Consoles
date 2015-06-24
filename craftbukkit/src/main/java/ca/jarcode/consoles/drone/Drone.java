package ca.jarcode.consoles.drone;

import ca.jarcode.consoles.computer.interpreter.types.LuaChest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public interface Drone {
	// all of these methods are allowed to block and are called from Lua program threads
	// TODO: Implement a drone. This abstraction is here because I don't know what to make a drone with atm.
	public void move(BlockFace face);
	public Location getLocation();
	public void remove();
	public void mine(BlockFace face);
	public void use(BlockFace face);
	public Material getType(BlockFace face);
	public LuaChest open(BlockFace face);
	public LuaChest inventory();
}
