package jarcode.consoles;

import jarcode.consoles.util.gson.LocationTypeAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageConsoleHandler {

	private static ImageConsoleHandler instance;

	public static ImageConsoleHandler getInstance() {
		return instance;
	}

	List<ImageConsole> imageConsoles = new ArrayList<>();

	private File file;
	private final Gson gson;

	private final LocationTypeAdapter locationTypeAdapter = new LocationTypeAdapter();

	public ImageConsoleHandler() {
		Consoles plugin = Consoles.getInstance();
		gson = new GsonBuilder()
				.registerTypeAdapter(ConsoleMeta.class, new JsonSerializer<ConsoleMeta>() {
					@Override
					public JsonElement serialize(ConsoleMeta consoleMeta, Type type,
					                             JsonSerializationContext jsonSerializationContext) {
						JsonObject object = new JsonObject();
						object.add("location",
								locationTypeAdapter.serialize(consoleMeta.location, null, jsonSerializationContext));
						object.add("face", new JsonPrimitive(consoleMeta.face.name()));
						object.add("w", new JsonPrimitive(consoleMeta.w));
						object.add("h", new JsonPrimitive(consoleMeta.h));
						return object;
					}
				})
				.registerTypeAdapter(ConsoleMeta.class, new JsonDeserializer<ConsoleMeta>() {
					@Override
					public ConsoleMeta deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
						JsonObject object = jsonElement.getAsJsonObject();
						Location location = locationTypeAdapter.deserialize(object.get("location"),
								null, jsonDeserializationContext);
						BlockFace face = BlockFace.valueOf(object.get("face").getAsString());
						int w = object.get("w").getAsInt();
						int h = object.get("h").getAsInt();
						return new ConsoleMeta(location, face, w, h);
					}
				})
				.enableComplexMapKeySerialization()
				.setPrettyPrinting()
				.create();

		file = new File(plugin.getDataFolder().getAbsolutePath() +
				File.separator + "images.json");
		if (!file.exists()) {
			File folder = plugin.getDataFolder();
			if (!folder.exists() && !folder.mkdir()) {
				plugin.getLogger().warning("Could not create plugin folder");
			}
			try {
				if (!file.createNewFile()) {
					plugin.getLogger().warning("Could not create JSON scoreboard file");
				}
				else {
					FileOutputStream fos = null;
					try {
						fos = new FileOutputStream(file);
						fos.write("{}".getBytes());
					}
					catch (IOException e) {
						e.printStackTrace();
					}
					finally {
						if (fos != null) {
							try {
								fos.flush();
								fos.close();
							}
							catch (IOException ignored) {}
						}
					}
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				plugin.getLogger().warning("Could not create JSON scoreboard file");
			}
		}
		instance = this;
	}
	public void load() {
		FileReader reader = null;
		List<ConsoleMeta> metaList = null;
		List<String> urls = null;
		try {
			reader = new FileReader(file);
			JsonParser parser = new JsonParser();
			JsonElement root = parser.parse(reader);
			JsonObject obj = root.getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
				if (entry.getKey().equals("consoleData")) {
					Type type = new TypeToken<ArrayList<ConsoleMeta>>(){}.getType();
					metaList = gson.fromJson(entry.getValue(), type);
				}
				else if (entry.getKey().equals("urls")) {
					Type type = new TypeToken<ArrayList<String>>(){}.getType();
					urls = gson.fromJson(entry.getValue(), type);
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (reader != null)
					reader.close();
			}
			catch (IOException ignored) {}
		}
		if (metaList != null && urls != null && metaList.size() == urls.size()) {
			for (int t = 0; t < metaList.size(); t++) {
				Location loc = metaList.get(t).location;
				BlockFace face = metaList.get(t).face;
				try {
					imageConsoles.add(new ImageConsole(new URL(urls.get(t)), face, loc, false));
				} catch (MalformedURLException e) {
					Bukkit.getLogger().severe("Could not load image console: ");
					e.printStackTrace();
				}
			}
			for (ImageConsole console : imageConsoles) {
				console.create(false);
			}
		}
		else {
			Consoles.getInstance().getLogger()
					.severe("Image consoles could not be reconstructed due to invalid JSON data.");
		}
	}

	public void save() {

		Type consoleDataType = new TypeToken<ArrayList<ConsoleMeta>>(){}.getType();
		Type stringListType = new TypeToken<ArrayList<String>>(){}.getType();

		ArrayList<ConsoleMeta> metaList = new ArrayList<>();
		ArrayList<String> urls = new ArrayList<>();

		for (ImageConsole console : imageConsoles) {
			metaList.add(console.console.createMeta());
			urls.add(console.url.toString());
		}

		FileOutputStream out = null;
		try {
			JsonObject obj = new JsonObject();
			obj.add("consoleData", gson.toJsonTree(metaList, consoleDataType));
			obj.add("urls", gson.toJsonTree(urls, stringListType));
			String bytes = obj.toString();
			out = new FileOutputStream(file);
			out.write(bytes.getBytes());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				}
				catch (IOException ignored) {}
			}
		}
	}
	public List<ImageConsole> getImageConsoles() {
		return imageConsoles;
	}
}
