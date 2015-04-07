package jarcode.consoles.util.gson;

import jarcode.consoles.util.LocalPosition;
import org.bukkit.craftbukkit.libs.com.google.gson.*;

import java.lang.reflect.Type;

public class LocalPositionTypeAdapter implements JsonSerializer<LocalPosition>, JsonDeserializer<LocalPosition> {
	@Override
	public LocalPosition deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		JsonObject obj = jsonElement.getAsJsonObject();
		int x = obj.get("x").getAsInt();
		int y = obj.get("y").getAsInt();
		int z = obj.get("z").getAsInt();
		return new LocalPosition(x, y, z);

	}

	@Override
	public JsonElement serialize(LocalPosition location, Type type, JsonSerializationContext jsonSerializationContext) {
		JsonObject obj = new JsonObject();
		obj.add("x", new JsonPrimitive(location.x));
		obj.add("y", new JsonPrimitive(location.y));
		obj.add("z", new JsonPrimitive(location.z));
		return obj;
	}
}
