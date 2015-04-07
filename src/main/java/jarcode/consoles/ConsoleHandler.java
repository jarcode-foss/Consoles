package jarcode.consoles;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jarcode.consoles.bungee.ConsoleBungeeHook;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import jarcode.consoles.util.LocalPosition;
import jarcode.consoles.util.PacketUtils;
import jarcode.consoles.util.Region;
import net.minecraft.server.v1_8_R1.*;
import net.minecraft.server.v1_8_R1.World;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.block.CraftCommandBlock;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftItemFrame;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ConsoleHandler implements Listener {

	// allow 20 index space for whatever
	private static final short STARTING_INDEX = 20;
	private static final ConsoleHandler INSTANCE = new ConsoleHandler();
	private static final boolean USE_BUNGEE = true;


	private static final Field PACKET_LIST;
	private static final Field PACKET_ENTITY_ID;

	static {
		try {
			PACKET_LIST = PacketPlayOutEntityMetadata.class.getDeclaredField("b");
			PACKET_LIST.setAccessible(true);
			PACKET_ENTITY_ID = PacketPlayOutEntityMetadata.class.getDeclaredField("a");
			PACKET_ENTITY_ID.setAccessible(true);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static {
		INSTANCE.paintThread.start();
	}

	private static Object get(Field field, Object instance) {
		try {
			return field.get(instance);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static ConsoleHandler getInstance() {
		return INSTANCE;
	}
	public static boolean isRegistered(CommandBlock block) {
		try {
			Field field = CraftCommandBlock.class.getDeclaredField("commandBlock");
			field.setAccessible(true);
			TileEntityCommand entity = (TileEntityCommand) field.get(block);
			Object obj = entity.getCommandBlock();
			return !(obj instanceof TileEntityCommandListener);
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static boolean registerListener(CommandBlock block, ConsoleListener listener) {
		try {
			Field field = CraftCommandBlock.class.getDeclaredField("commandBlock");
			field.setAccessible(true);
			TileEntityCommand entity = (TileEntityCommand) field.get(block);
			Object obj = entity.getCommandBlock();
			if (obj instanceof TileEntityCommandListener) {
				TileEntityCommandListener old = (TileEntityCommandListener) obj;
				field.set(entity, new ConsoleCommandBlockListener(old, listener));
				return true;
			}
			else return false;
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static boolean restoreCommandBlock(CommandBlock block) {
		try {
			Field field = CraftCommandBlock.class.getDeclaredField("commandBlock");
			field.setAccessible(true);
			TileEntityCommand entity = (TileEntityCommand) field.get(block);
			Object obj = entity.getCommandBlock();
			if (obj instanceof ConsoleCommandBlockListener) {
				ConsoleCommandBlockListener listener = (ConsoleCommandBlockListener) obj;
				field.set(entity,listener.tileEntity);
				return true;
			}
			else return false;
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return false;
	}
	// thread-safe array list
	CopyOnWriteArrayList<ManagedConsole> consoles = new CopyOnWriteArrayList<>();
	// we lock allocation code because it has to be accessed from the painting thread to send packets
	private final Object ALLOCATION_LOCK = new Object();
	// this holds all indexes of the maps that the server refers to
	private ArrayList<Short> defaultAllocation = new ArrayList<>();
	// this is the index mappings for every client
	private Map<String, BiMap<Short, Short>> allocations = new HashMap<>();
	// the painter (Runnable) ran in separate thread that handles requests/paints
	private final MapPainter painter = new MapPainter();
	// the single thread used for painting
	private final Thread paintThread = new Thread(painter);

	private ConsoleBungeeHook hook;

	public boolean local = !USE_BUNGEE;
	{
		paintThread.setDaemon(true);
		paintThread.setName("Console Painting Thread");
	}

	public void setHook(ConsoleBungeeHook hook) {
		this.hook = hook;
	}

	public short translateIndex(String context, short global) {
		synchronized (ALLOCATION_LOCK) {
			if (local)
				return getIndexTable(context).get(global);
			else return grabIndex(context, global);
		}
	}
	private short grabIndex(String context, short global) {
		Short s = allocations.containsKey(context) ? allocations.get(context).get(global) : Short.valueOf((short) 0);
		return s == null ? 0 : s;
	}
	// this takes all the currently allocated values for this context and
	// remaps them to -ive keys, and re-allocates all the global values to
	// new context values
	//
	// used to refresh map damage values after a dimension change
	//
	// returns true if successful, false if there is no room to allocate
	public boolean allocateNew(String context) {
		if (!Thread.holdsLock(ALLOCATION_LOCK)) synchronized (ALLOCATION_LOCK) {
			return allocateNew(context);
		}
		Map<Short, Short> target = getIndexTable(context);
		Map<Short, Short> old = HashBiMap.create();
		for (Map.Entry<Short, Short> entry : target.entrySet()) {
			for (short t = -1; ; t--) {
				if (!old.containsKey(t)) {
					old.put(t, entry.getValue());
					break;
				}
			}
		}
		if (!local) {
			short[] arr = new short[target.size()];
			int i = 0;
			for (short s : target.values()) {
				arr[i] = s;
				i++;
			}
			hook.forwardIds(context, arr);
		}
		target.clear();
		for (Map.Entry<Short, Short> entry : old.entrySet()) {
			target.put(entry.getKey(), entry.getValue());
		}
		for (short index : defaultAllocation) {
			short to = mapIndex(context, index);
			if (to == Short.MAX_VALUE) return false;
		}
		return true;
	}
	// this is used to add new indexes from created maps
	// if the global value already exists, nothing will happen,
	// and the current key is returned.
	public short mapIndex(String context, short global) {
		if (!Thread.holdsLock(ALLOCATION_LOCK)) synchronized (ALLOCATION_LOCK) {
			return mapIndex(context, global);
		}
		Map<Short, Short> target = getIndexTable(context);
		if (target.containsKey(global)) return target.get(global);
		short result;
		for (short t = STARTING_INDEX;; t++) {
			if (!target.containsValue(t)) {
				target.put(global, t);
				result = t;
				break;
			}
		}
		if (!local)
			hook.forwardIds(context, result);
		return result;
	}
	private BiMap<Short, Short> getIndexTable(String context) {
		if (!allocations.containsKey(context)) {
			BiMap<Short, Short> target = createTable();
			if (!local) {
				short[] arr = new short[target.size()];
				int i = 0;
				for (short s : target.values()) {
					arr[i] = s;
					i++;
				}
				hook.forwardIds(context, arr);
			}
			allocations.put(context, target);
			return target;
		}
		else return allocations.get(context);
	}
	// when we create new allocation mappings for a client, we can copy over
	// the global/default allocations
	private BiMap<Short, Short> createTable() {
		BiMap<Short, Short> target = HashBiMap.create();
		for (short s : defaultAllocation) {
			target.put(s, s);
		}
		return target;
	}
	public MapPainter getPainter() {
		return painter;
	}
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent e) {
		World world = ((CraftWorld) e.getWorld()).getHandle();
		for (Entity entity : e.getChunk().getEntities()) {
			for (ManagedConsole console : consoles) {
				console.bukkitEntities().stream()
						.filter(frame -> entity == frame)
						.forEach(frame -> {
							EntityItemFrame nms = ((CraftItemFrame) frame).getHandle();
							world.removeEntity(nms);
						});
			}
		}
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			org.bukkit.Chunk chunk = e.getChunk();
			Region region = new Region(new LocalPosition(chunk.getX() * 16, 0, chunk.getZ() * 16),
					new LocalPosition(chunk.getX() * 16 + 15, 0, chunk.getZ() * 16 + 15));
			for (ManagedConsole console : consoles) {
				console.bukkitEntities().stream()
						.filter(frame -> frame.getWorld() == e.getWorld())
						.filter(frame -> region.insideIgnoreY(frame.getLocation()))
						.forEach(frame -> {
							EntityItemFrame nms = ((CraftItemFrame) frame).getHandle();
							World world = ((CraftWorld) e.getWorld()).getHandle();
							if (!world.addEntity(nms)) {
								Consoles.getInstance().getLogger().severe("Failed to spawn console item frame: "
										+ frame.getLocation().toString() + ", identifier: " + console.getIdentifier());
							} else if (Consoles.DEBUG) {
								Consoles.getInstance().getLogger().info("Spawned item frame: "
										+ frame.getLocation().toString() + ", identifier: " + console.getIdentifier());
							}
						});
			}
		});
	}
	@EventHandler
	public void onPlayerWorldChange(PlayerChangedWorldEvent e) {
		shiftMapValues(e.getPlayer());
	}
	// dimension change on respawn bug
	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent e) {
		shiftMapValues(e.getPlayer());
	}
	// here's the ultimate fix for the static map bug that's been annoying me:
	public void shiftMapValues(final Player player) {
		// shift all the current map IDs to new values for this client
		allocateNew(player.getName());
		// toggle all of the map sections for every console for this context
		for (final ManagedConsole console : consoles) {
			// toggle switches
			getPainter().toggle(console, player.getName());
			// request repaint
			doLater(() -> getPainter().updateFor(console, player, false, true), 5L);
		}
	}
	public void blacklist(Player player, short[] ids) {
		if (!Thread.holdsLock(ALLOCATION_LOCK)) synchronized (ALLOCATION_LOCK) {
			blacklist(player, ids);
		}
		BiMap<Short, Short> target = HashBiMap.create();

		List<Short> toMap = new ArrayList<>();

		short n = -1;
		for (short id : ids) {
			for (; ; n--) {
				if (!target.containsKey(n)) {
					// a blacklisted id is already in use, remap it
					if (target.containsValue(id)) {
						short key = target.inverse().get(id);
						toMap.add(key);
						target.remove(key);
					}
					target.put(n, id);
					break;
				}
			}
		}
		allocations.put(player.getName(), target);
		for (short s : defaultAllocation) {
			mapIndex(player.getName(), s);
		}
		for (short s : toMap) {
			mapIndex(player.getName(), s);
		}
	}
	private void doLater(Runnable runnable) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), runnable);
	}
	private void doLater(Runnable runnable, long delay) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), runnable, delay);
	}
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		PacketUtils.registerOutListener(PacketPlayOutEntityMetadata.class, e.getPlayer(),
				packet -> handleMetadataPacket(packet, e.getPlayer().getName()));
	}
	// this baby translates map IDs in outgoing packets according to the player
	@SuppressWarnings("unchecked")
	private boolean handleMetadataPacket(PacketPlayOutEntityMetadata packet, String context) {
		// get list of objects
		List<WatchableObject> list = (List<WatchableObject>) get(PACKET_LIST, packet);
		// create object mappings of the above list
		TIntObjectMap map = new TIntObjectHashMap(10, 0.5F, -1);
		for (WatchableObject aList : list) {
			map.put(aList.a(), aList);
		}
		// get entity id
		int id;
		try {
			id = PACKET_ENTITY_ID.getInt(packet);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		// if the map/packet contains an item stack, and is for a console entity
		if (map.containsKey(8) && isConsoleEntity(id)) {
			// get stack from the map
			ItemStack stack = (ItemStack) ((WatchableObject) map.get(8)).b();
			// global map id
			short global = (short) stack.getData();
			// player context map id
			short translated = translateIndex(context, global);
			// set data of item stack
			stack.setData(translated);
		}
		// block other map metadata
		else if (map.containsKey(8) && ((WatchableObject) map.get(8)).b() instanceof ItemStack) {
			ItemStack stack = (ItemStack) ((WatchableObject) map.get(8)).b();
			if (stack.getItem() == Items.FILLED_MAP || stack.getItem() == Items.MAP) {
				return false;
			}
			System.out.println("Blocked non-console item frame metadata packet");
		} else if (map.containsKey(8) && ((WatchableObject) map.get(8)).b() == null) {
			System.out.println("Attempted to send metadata packet with null item stack!");
			return false;
		}
		return true;
	}
	public short[] getContextIds(Player player) {
		synchronized (ALLOCATION_LOCK) {
			if (!allocations.containsKey(player.getName())) return new short[0];
			Map<Short, Short> map = getIndexTable(player.getName());
			short[] arr = new short[map.size()];
			int index = 0;
			for (short s : map.values()) {
				arr[index] = s;
				index++;
			}
			return arr;
		}
	}
	public void clearAllocations(Player player) {
		synchronized (ALLOCATION_LOCK) {
			if (allocations.containsKey(player.getName()))
				allocations.remove(player.getName());
			getIndexTable(player.getName());
		}
	}
	// remove all the allocated maps for the player
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		if (local)
			clearAllocations(e.getPlayer());
	}
	// block maps
	@EventHandler
	public void onItemCraft(CraftItemEvent e) {
		if (e.getRecipe().getResult().getType() == Material.MAP
				|| e.getRecipe().getResult().getType() == Material.EMPTY_MAP)
			e.setCancelled(true);
	}

	// block maps
	@EventHandler
	public void onItemCreative(InventoryCreativeEvent e) {
		if (e.getCurrentItem().getType() == Material.MAP || e.getCursor().getType() == Material.MAP ||
				e.getCurrentItem().getType() == Material.EMPTY_MAP || e.getCursor().getType() == Material.EMPTY_MAP)
			e.setCancelled(true);
	}

	@EventHandler
	public void onEntityInteract(PlayerInteractEntityEvent e) {
		if (e.getRightClicked() instanceof ItemFrame && isConsoleEntity((ItemFrame) e.getRightClicked())) {
			e.setCancelled(true);
		}
	}
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof ItemFrame && isConsoleEntity((ItemFrame) e.getEntity()))
			e.setCancelled(true);
	}
	@EventHandler
	public void onFrameBreak(HangingBreakEvent e) {
		if (e.getEntity() instanceof ItemFrame && isConsoleEntity((ItemFrame) e.getEntity())) {
			e.setCancelled(true);
		}
	}
	@EventHandler
	public void onMapInit(MapInitializeEvent e) {
	}
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		for (ManagedConsole console : consoles.toArray(new ManagedConsole[consoles.size()])) {
			if (console.created()) {
				int[] arr = console.intersect(e.getPlayer().getEyeLocation(), 7);
				if (arr != null) {
					console.handleClick(arr[0], arr[1], e.getPlayer());
				}
			}
		}
	}
	public ManagedConsole[] getConsoles() {
		return consoles.toArray(new ManagedConsole[consoles.size()]);
	}
	@EventHandler
	public void onPluginDisable(PluginDisableEvent e) {
		removeAll();
	}
	public boolean isConsoleEntity(ItemFrame entity) {
		for (ManagedConsole console : consoles) {
			if (console.protect(entity))
				return true;
		}
		return false;
	}
	public boolean isConsoleEntity(int id) {
		for (ManagedConsole console : consoles) {
			if (console.isFrameId(id))
				return true;
		}
		return false;
	}
	public ManagedConsole getConsoleForId(int id) {
		for (ManagedConsole console : consoles) {
			if (console.isFrameId(id))
				return console;
		}
		return null;
	}
	public void removeAll() {
		for (ManagedConsole console : consoles) {
			console.remove(false);
			handleRemove(console, false);
		}
		consoles.clear();
	}
	void handleRemove(ManagedConsole console) {
		handleRemove(console, true);
	}
	private void handleRemove(ManagedConsole console, boolean rm) {
		if (console.created()) {
			int size = console.getFrameWidth() * console.getFrameHeight();
			synchronized (ALLOCATION_LOCK) {
				for (short t = console.getMapIndex(); t < console.getMapIndex() + size; t++) {
					// update context allocations
					for (Map<Short, Short> map : allocations.values()) {
						map.remove(t);
					}
					defaultAllocation.remove(Short.valueOf(t));
				}
			}
		}
		if (rm)
			consoles.remove(console);
	}
	// this allocates a block of indexes for the maps to use,
	// and updates all the allocation mappings for active contexts.
	short allocate(int size) {
		short lowest = STARTING_INDEX;
		synchronized (ALLOCATION_LOCK) {
			while (!fits(lowest, size))
				lowest++;
			for (short t = lowest; t < lowest + size; t++) {
				// update context allocations
				for (String context : allocations.keySet()) {
					if (getIndexTable(context).containsKey(t))
						System.out.println("Warning, overwriting map index allocation for context: " + context);
					mapIndex(context, t);
				}
				defaultAllocation.add(t);
			}
		}
		return lowest;
	}
	private boolean fits(short i, int size) {
		for (short index : defaultAllocation) {
			if (index >= i && index < i + size) return false;
		}
		return true;
	}
	public List<ManagedConsole> get(String identifier) {
		return consoles.stream()
				.filter(console -> console.getIdentifier() != null && console.getIdentifier().equals(identifier))
				.collect(Collectors.toList());
	}
}
