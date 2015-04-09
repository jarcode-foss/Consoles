package jarcode.consoles.util;

import net.minecraft.server.v1_8_R2.*;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R2.CraftServer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

// This is meant to break a map
public class MapInjector extends WorldMap {

	private static final Field PERSISTENT_COLLECTION_MAP;

	static {
		try {
			PERSISTENT_COLLECTION_MAP = PersistentCollection.class.getDeclaredField("a");
			PERSISTENT_COLLECTION_MAP.setAccessible(true);
		}
		catch (Exception e) {
			throw new RuntimeException();
		}
	}

	// this is another terrible method. Not as much as the second one, but still terrible
	// there's no way to fix a map once it has been overridden, but the second method just kill map
	// functionality entirely anyway.
	public static void overrideMap(int id) {
		World mainWorld = ((CraftServer) Bukkit.getServer()).getServer().worlds.get(0);
		try {
			// register the fake map
			MapInjector fake = new MapInjector(id);
			mainWorld.a("map_" + id, fake);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	// don't try this at home
	// very bad programming practise to do this, but there's really no other way to remove default render functionality.
	// we're overriding a final static type to change the map packet managing and serialization/deserialization.
	public static void injectTypes() {
		try {
			Field field = Items.class.getDeclaredField("FILLED_MAP");

			// reflect the reflect library. I know, this is ridiculous.
			// java is such a stupid language
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			// remove the final flag on the security int/bytes
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			// replace the value
			field.set(null, new FakeItemWorldMap());

			// There are a number of things wrong with this, particularity that the JVM optimizes and works around
			// the fact that fields are final or not. You can't do this on compile-time constants (like strings
			// and primitives), and will not work under most security managers.
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static int clearNBTMapFiles() {
		int failed = 0;
		String worldName = ((CraftServer) Bukkit.getServer()).getServer()
				.getPropertyManager().getString("level-name", "world");
		File world = new File(Bukkit.getWorldContainer().getAbsolutePath() + File.separatorChar
				+ worldName + File.separatorChar + "data");
		if (!world.isDirectory()) return -1;
		File[] arr = world.listFiles();
		if (arr == null) return -2;
		for (File file : arr) {
			if (file.getName().startsWith("map_") && !file.isDirectory()) {
				try {
					FileUtils.forceDelete(file);
				} catch (IOException e) {
					failed++;
				}
			}
		}
		return failed;
	}

	private int id;
	public MapInjector(int id) {
		super("map_" + id);
		this.id = id;
		this.map = 0;
		this.scale = 0;
	}
	// This handles cursors for the map. Safe to remove entirely.
	@Override
	public void a(EntityHuman entityhuman, ItemStack itemstack) {}
	// map loading, we're not concerned about any data here.
	@Override
	public void a(NBTTagCompound nbttagcompound) {}
	// map saving, we just ensure data is saved in a valid format
	// this prevents malformed NBT data.
	@Override
	public void b(NBTTagCompound nbttagcompound) {
		nbttagcompound.setByte("dimension", (byte) 0);
		nbttagcompound.setInt("xCenter", 0);
		nbttagcompound.setInt("zCenter", 0);
		nbttagcompound.setByte("scale", (byte) 0);
		nbttagcompound.setShort("width", (short) 128);
		nbttagcompound.setShort("height", (short) 128);
		nbttagcompound.setByteArray("colors", this.colors);
	}
	// cursor packet, just send nothing
	@Override
	public Packet a(ItemStack itemstack, World world, EntityHuman entityhuman) {
		return null;
	}
	// create fake tracker
	@Override
	@SuppressWarnings("unchecked")
	public WorldMapHumanTracker a(EntityHuman entityhuman) {
		WorldMapHumanTracker worldmaphumantracker = this.i.get(entityhuman);
		if(worldmaphumantracker == null) {
			worldmaphumantracker = new FakeTracker(entityhuman);
			this.i.put(entityhuman, worldmaphumantracker);
			this.g.add(worldmaphumantracker);
		}

		return worldmaphumantracker;
	}
	public static class FakeItemWorldMap extends ItemWorldMap {
		// return fake maps
		@Override
		public WorldMap getSavedMap(ItemStack itemstack, World world) {
			return new MapInjector(itemstack.getData());
		}
		// render method, do nothing.
		@Override
		public void a(World world, Entity entity, WorldMap worldmap) {}
		// track method, do nothing.
		@Override
		public void a(ItemStack itemstack, World world, Entity entity, int i, boolean flag) {}
		// send map data, send dummy packet
		@Override
		public Packet c(ItemStack itemstack, World world, EntityHuman entityhuman) {
			return null;
		}
		// handle map NBT data, remove tags.
		@Override
		public void d(ItemStack itemstack, World world, EntityHuman entityhuman) {
			if(itemstack.hasTag()) {
				itemstack.getTag().setBoolean("map_is_scaling", false);
				itemstack.getTag().set("Decorations", new NBTTagList());
			}
		}
	}
	public class FakeTracker extends WorldMapHumanTracker {
		public FakeTracker(EntityHuman entityhuman) {
			super(entityhuman);
		}
		// tracker/update packet, send nothing
		@Override
		public Packet a(ItemStack itemstack) {
			return null;
		}
	}
}
