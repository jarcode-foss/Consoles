package ca.jarcode.consoles.computer;

import ca.jarcode.consoles.Consoles;
import ca.jarcode.consoles.util.Position2D;
import ca.jarcode.consoles.event.bukkit.MapUpdateEvent;
import ca.jarcode.consoles.util.Allocation;
import ca.jarcode.consoles.util.ChunkMapper;
import ca.jarcode.consoles.util.InstanceListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static ca.jarcode.consoles.Lang.lang;

public class MapDataStore {

	private static final Map<String, MapDataStore[]> levels = new HashMap<>();
	public static final int LEVEL_COUNT = 4;
	public static final String MAP_FOLDER = "maps";

	private static final int UPDATE_THRESHOLD = 500;

	public static void init(Plugin plugin) {
		InstanceListener listener = new InstanceListener();
		listener.chain(MapDataStore::handle)
				.register(BlockPlaceEvent.class)
				.register(BlockBreakEvent.class)
				.register(PlayerJoinEvent.class)
				.register(PlayerMoveEvent.class);
		listener.register(WorldLoadEvent.class, (e) -> init(plugin, e.getWorld(), 0, 0));
		listener.register(WorldUnloadEvent.class, (e) -> {
			save(plugin, e.getWorld());
			levels.remove(e.getWorld().getName());
		});
		listener.register(PluginDisableEvent.class, (e) -> {
			if (e.getPlugin() == plugin) for (World w : Bukkit.getWorlds())
				save(plugin, w);
			levels.clear();
		});
		listener.associate(plugin);
		for (World world : Bukkit.getWorlds()) {
			init(plugin, world, 0, 0);
		}
	}
	public static void handle(Event event) {
		if (event instanceof PlayerEvent) {
			Location loc = ((PlayerEvent) event).getPlayer().getLocation();
			update(((PlayerEvent) event).getPlayer().getWorld(), loc.getBlockX(), loc.getBlockZ());
		}
		else if (event instanceof BlockEvent) {
			update(((BlockEvent) event).getBlock().getWorld(),
					((BlockEvent) event).getBlock().getX(), ((BlockEvent) event).getBlock().getZ());
		}
	}
	public static void init(Plugin plugin, World world, int originX, int originZ) {
		plugin.getLogger().info(String.format(lang.getString("loading-map-data"), world.getName()));
		MapDataStore[] stores = new MapDataStore[LEVEL_COUNT];
		for (int t = 0; t < stores.length; t++) {
			stores[t] = new MapDataStore(world, originX, originZ, t);
			File file = new File(plugin.getDataFolder().getAbsolutePath()
					+ File.separator + MAP_FOLDER + File.separator + "map-" + t + "-" + world.getName() + ".dat");
			if (!file.exists()) {
				File folder = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + MAP_FOLDER);
				if (!folder.exists())
					if (!folder.mkdir())
						plugin.getLogger().severe(String.format(lang.getString("folder-create-fail"),
								folder.getAbsolutePath()));
				plugin.getLogger().info(String.format(lang.getString("file-create-fallback"),
						"map-" + t + "-" + world.getName() + ".dat"));
				try {
					if (!file.createNewFile())
						plugin.getLogger().severe(String.format(lang.getString("file-create-fail"),
								file.getAbsolutePath()));
				} catch (IOException e) {
					plugin.getLogger().severe(String.format(lang.getString("file-create-fail"), file.getAbsolutePath()));
					e.printStackTrace();
				}
			}
			else
				loadFor(stores[t], file);
		}
		levels.put(world.getName(), stores);
	}

	public static void save(Plugin plugin, World world) {
		plugin.getLogger().info(String.format(lang.getString("saving-map-data"), world.getName()));
		MapDataStore[] stores = levels.get(world.getName());
		if (stores == null) {
			plugin.getLogger().warning(String.format(lang.getString("missing-map-data"), world.getName()));
			return;
		}
		for (int t = 0; t < stores.length; t++) {
			File file = new File(plugin.getDataFolder().getAbsolutePath()
					+ File.separator + MAP_FOLDER + File.separator + "map-" + t + "-" + world.getName() + ".dat");
			saveFor(stores[t], file);
		}
	}

	private static void loadFor(MapDataStore store, File file) {
		try (FileInputStream in = new FileInputStream(file)) {
			DataInputStream din = new DataInputStream(in);
			// error suppression for blank files.
			// I still need to fix this bug.
			if (din.available() == 0) return;
			int amt = din.readInt();
			while (amt > 0) {
				int x = din.readInt();
				int y = din.readInt();
				ChunkMapper.PreparedMapSection section = new ChunkMapper.PreparedMapSection();
				for (int t = 0; t < 128; t++)
					for (int j = 0; j < 128; j++)
						section.colors[t + (j * 128)] = din.readByte();
				store.map.put(new Position2D(x, y), section);
				amt--;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void saveFor(MapDataStore store, File file) {
		try (DataOutputStream dout = new DataOutputStream(new FileOutputStream(file))) {
			dout.writeInt(store.map.size());
			for (Map.Entry<Position2D, ChunkMapper.PreparedMapSection> entry : store.map.entrySet()) {
				dout.writeInt(entry.getKey().getX());
				dout.writeInt(entry.getKey().getY());
				for (int t = 0; t < 128; t++)
					for (int j = 0; j < 128; j++)
						dout.writeByte(entry.getValue().colors[t + (j * 128)]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// all translations needed to cover adjacent sections, including the tile itself
	private static final Position2D[] MODS = {
			new Position2D(1, 0), new Position2D(0, 1),
			new Position2D(-1, 0), new Position2D(0, -1),
			new Position2D(1, 1), new Position2D(-1, 1),
			new Position2D(-1, -1), new Position2D(1, -1),
			new Position2D(0, 0)
	};

	public static MapDataStore[] getStores(World world) {
		// initialize for this world
		if (!levels.containsKey(world.getName())) {
			init(Consoles.getInstance(), world, 0, 0);
		}
		return MapDataStore.levels.get(world.getName());
	}

	public static void update(World world, int x, int z) {
		// initialize for this world
		if (!levels.containsKey(world.getName())) {
			init(Consoles.getInstance(), world, 0, 0);
		}
		for (int t = 0; t < LEVEL_COUNT; t++)
			MapDataStore.levels.get(world.getName())[t].update(x, z);
	}

	// map of all the sections, the position corresponds with the corner of the
	// map (in global coordinates), the section only contains the memory.
	public final HashMap<Position2D, ChunkMapper.PreparedMapSection> map = new HashMap<>();
	public final int originX, originZ;
	public final int scale;
	public final int sectionSize, middleOffset;
	public final World world;

	private long lastUpdate = 0;

	public MapDataStore(World world, int originX, int originY, int scale) {
		this.originX = originX;
		this.originZ = originY;
		this.scale = scale;
		this.world = world;
		sectionSize = 128 << scale;
		middleOffset = sectionSize >> 2;
	}

	// global coordinates
	public ChunkMapper.PreparedMapSection getAt(int x, int y) {
		Map.Entry<Position2D, ChunkMapper.PreparedMapSection> ret = map.entrySet().stream()
				.filter(entry -> new Allocation(entry.getKey().getX(),
						entry.getKey().getY(), sectionSize, sectionSize).inside(x, y))
				.findFirst()
				.orElseGet(() -> null);
		return ret == null ? null : ret.getValue();
	}
	// global coordinates
	public Map.Entry<Position2D, ChunkMapper.PreparedMapSection> createAt(int x, int y) {

		// here, we're aligning our maps to a grid according to the origin this
		// infinite map view was created with.

		// subtract this view's origin, shift seven bits right (divide by 128), and then by the scale.
		int xo = ((x - originX) >> 7 >> scale);
		int yo = ((y - originZ) >> 7 >> scale);

		// calculate the corner of this map, in global coordinates.
		Position2D corner = new Position2D(
				originX + (xo * sectionSize),
				originZ + (yo * sectionSize)
		);

		ChunkMapper.PreparedMapSection section = new ChunkMapper.PreparedMapSection();
		map.put(corner, section);
		return new AbstractMap.SimpleEntry<>(corner, section);
	}

	// global coordinates
	public void update(int x, int y) {

		if (System.currentTimeMillis() - lastUpdate < UPDATE_THRESHOLD)
			return;
		lastUpdate = System.currentTimeMillis();

		AtomicBoolean flag = new AtomicBoolean();

		// check section and all adjacent sections
		for (Position2D mod : MODS) {
			// create and update the section if it doesn't exist
			int rx = x + (mod.getX() * sectionSize);
			int ry = y + (mod.getY() * sectionSize);
			if (getAt(rx, ry) == null)
				createAt(rx, ry);
		}
		// update all other sections
		map.entrySet().stream()
				.forEach(entry -> {
					if (ChunkMapper.updateSection(entry.getValue(), ((CraftWorld) world).getHandle(),
							entry.getKey().getX() + middleOffset,
							entry.getKey().getY() + middleOffset,
							x, y, scale))
						flag.set(true);
				});
		if (flag.get()) {
			Consoles.getInstance().getServer().getPluginManager().callEvent(new MapUpdateEvent(world, x, y, scale));
		}
	}
}
