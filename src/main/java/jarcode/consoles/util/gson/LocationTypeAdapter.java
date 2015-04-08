package jarcode.consoles.util.gson;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import com.google.gson.*;

import java.lang.reflect.Type;

public class LocationTypeAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {
	@Override
	public Location deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		JsonObject obj = jsonElement.getAsJsonObject();
		String worldName = obj.get("world").getAsString();
		double x = obj.get("x").getAsDouble();
		double y = obj.get("y").getAsDouble();
		double z = obj.get("z").getAsDouble();
		float yaw = obj.get("yaw").getAsFloat();
		float pitch = obj.get("pitch").getAsFloat();
		World world = Bukkit.getWorld(worldName);
		if (world == null) {
			if (Bukkit.getWorlds().size() == 0) {
				throw new JsonParseException("Cannot deserialize bukkit location without any worlds loaded!");
			}
			world = Bukkit.getWorlds().get(0);
		}
		return new Location(world, x, y, z, yaw, pitch);

	}

	@Override
	public JsonElement serialize(Location location, Type type, JsonSerializationContext jsonSerializationContext) {
		JsonObject obj = new JsonObject();
		obj.add("world", new JsonPrimitive(location.getWorld().getName()));
		obj.add("x", new JsonPrimitive(location.getX()));
		obj.add("y", new JsonPrimitive(location.getY()));
		obj.add("z", new JsonPrimitive(location.getZ()));
		obj.add("yaw", new JsonPrimitive(location.getYaw()));
		obj.add("pitch", new JsonPrimitive(location.getPitch()));
		return obj;
	}
}
