package jarcode.consoles.internal;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jarcode.consoles.Consoles;
import jarcode.consoles.computer.skript.ScriptInterface;
import jarcode.consoles.computer.skript.ScriptUploader;
import jarcode.consoles.messaging.ConsoleBungeeHook;
import jarcode.consoles.computer.ComputerHandler;
import jarcode.consoles.util.*;
import net.minecraft.server.v1_8_R3.*;
import net.minecraft.server.v1_8_R3.DataWatcher.WatchableObject;
import net.minecraft.server.v1_8_R3.World;
import org.bukkit.*;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.CommandBlock;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItemFrame;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/*

This handles a bunch of things:

- a good portion of misc events
- client <-> server frame ID translation and packet listeners
- plugin hooks

 */
@SuppressWarnings("unused")
public class ConsoleHandler implements Listener {

	private static final ConsoleHandler INSTANCE;

	private static final Field PACKET_LIST;
	private static final Field PACKET_ENTITY_ID;

	private static final HashMap<String, BiConsumer<Plugin, Logger>> HOOK_INITIALIZERS = new HashMap<>();

	static {
		HOOK_INITIALIZERS.put("Skript", (plugin, logger) -> {
			if (plugin != null) {
				try {
					ScriptInterface.set(new ScriptUploader(plugin));
				} catch (ScriptInterface.FailedHookException e) {
					logger.warning("Failed to hook into Skript plugin!");
					e.printStackTrace();
				}
			}
		});
	}

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
		INSTANCE = new ConsoleHandler();
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

	public boolean commandBlocksEnabled = ((CraftServer) Bukkit.getServer()).getServer().getEnableCommandBlock();

	// tracked map packets (these are not blocked!)
	private List<PacketPlayOutMap> packets = new ArrayList<>();

	private ConsoleBungeeHook hook;

	public boolean local = true;

	{
		paintThread.setDaemon(true);
		paintThread.setName("Console Painting Thread");
	}

	public ConsoleHandler() {

		((CraftServer) Bukkit.getServer()).getServer().getPropertyManager().setProperty("enable-command-block", true);

		HOOK_INITIALIZERS.entrySet().forEach((entry) -> {
			Plugin plugin = Bukkit.getPluginManager().getPlugin(entry.getKey());
			if (plugin != null)
				entry.getValue().accept(plugin, Consoles.getInstance().getLogger());
		});
	}

	public void setHook(ConsoleBungeeHook hook) {
		this.hook = hook;
	}

	public short translateIndex(String context, short global) {
		synchronized (ALLOCATION_LOCK) {
			if (!getIndexTable(context).containsKey(global))
				return -1;
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
		for (short t = Consoles.startingId;; t++) {
			if (!target.containsValue(t)) {
				target.put(global, t);
				result = t;
				break;
			}
		}
		return result;
	}
	private BiMap<Short, Short> getIndexTable(String context) {
		if (!allocations.containsKey(context)) {
			BiMap<Short, Short> target = createTable();
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
	public void wrapCommandBlocks(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null && e.getClickedBlock().getState() instanceof CommandBlock) {
			CommandBlockUtils.wrap((CommandBlock) e.getClickedBlock().getState());
		}
	}
	@EventHandler
	public void removeMaps(PlayerJoinEvent e) {
		Inventory inv = e.getPlayer().getInventory();
		for (int t = 0; t < inv.getSize(); t++) {
			if (inv.getItem(t) != null && (inv.getItem(t).getType() == Material.MAP
					|| inv.getItem(t).getType() == Material.EMPTY_MAP))
				inv.setItem(t, null);
		}
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
							if (Consoles.debug) {
								Consoles.getInstance().getLogger().info("Removed item frame: "
										+ frame.getLocation().toString() + ", identifier: " + console.getIdentifier());
							}
						});
			}
		}
	}
	@EventHandler
	public void restoreProperties(PluginDisableEvent e) {
		if (e.getPlugin() == Consoles.getInstance()) {
			((CraftServer) Bukkit.getServer()).getServer()
					.getPropertyManager().setProperty("enable-command-block", commandBlocksEnabled);
		}
	}
	@EventHandler
	public void loadAdjacentConsoles(PlayerJoinEvent e) {
		Chunk chunk = e.getPlayer().getLocation().getChunk();
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			int dist = Bukkit.getServer().getViewDistance();
			for (int x = -dist; x <= dist; x++) {
				for (int z = -dist; z < dist; z++) {
					Chunk at = e.getPlayer().getWorld().getChunkAt(chunk.getX() + x, chunk.getZ() + z);
					updateFrames(chunk);
				}
			}
		}, 5L);
	}
	private void updateFrames(Chunk chunk) {
		for (ManagedConsole console : consoles) {
			console.bukkitEntities().stream()
					.filter(frame -> frame.getWorld() == chunk.getWorld())
					.filter(frame -> {
						Chunk at = frame.getLocation().getChunk();
						return at.getX() == chunk.getX() && at.getZ() == chunk.getZ();
					})
					.filter(frame -> !Arrays.asList(chunk.getEntities()).contains(frame))
					.forEach(frame -> {
						EntityItemFrame nms = ((CraftItemFrame) frame).getHandle();
						World world = ((CraftWorld) chunk.getWorld()).getHandle();
						nms.dead = false;
						if (!world.addEntity(nms)) {
							Consoles.getInstance().getLogger().severe("Failed to spawn console item frame: "
									+ frame.getLocation().toString() + ", identifier: " + console.getIdentifier());
						} else if (Consoles.debug) {
							Consoles.getInstance().getLogger().info("Spawned item frame: "
									+ frame.getLocation().toString() + ", identifier: " + console.getIdentifier());
						}
					});
		}
	}
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		Chunk chunk = e.getChunk();
		updateFrames(chunk);
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
	public void replaceWithBlacklist(Player player, short[] ids) {
		if (!Thread.holdsLock(ALLOCATION_LOCK)) synchronized (ALLOCATION_LOCK) {
			replaceWithBlacklist(player, ids);
		}
		BiMap<Short, Short> target = HashBiMap.create();

		short n = -1;
		for (short id : ids) {
			for (; ; n--) {
				if (!target.containsKey(n)) {
					target.put(n, id);
					break;
				}
			}
		}
		allocations.put(player.getName(), target);
		for (short s : defaultAllocation) {
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
	public void onPlayerLogin(PlayerLoginEvent e) {
		// if this isn't a standalone server, we need to allocate new IDs if the player already has
		// allocated maps.
		if (!local && allocations.containsKey(e.getPlayer().getName()))
			allocateNew(e.getPlayer().getName());
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		PacketUtils.registerOutListener(PacketPlayOutEntityMetadata.class, e.getPlayer(),
				packet -> handleMetadataPacket(packet, e.getPlayer().getName()));
		PacketUtils.registerOutListener(PacketPlayOutMap.class, e.getPlayer(), this::handleMapPacket);
	}

	public PacketPlayOutMap newMapPacket() {
		PacketPlayOutMap packet = new PacketPlayOutMap();
		packets.add(packet);
		return packet;
	}

	// we're going to block all map packets other than the ones we send. This is
	// a last stand defence against other plugins that may send map packets manually.

	// Most packets should be blocked by our fake map items and trackers, so this
	// won't do much in normal servers.
	private boolean handleMapPacket(PacketPlayOutMap packet) {
		if (packets.contains(packet)) {
			packets.remove(packet);
			return true;
		}
		else return false;
	}
	// this baby translates map IDs in outgoing packets according to the player
	@SuppressWarnings("unchecked")
	private boolean handleMetadataPacket(PacketPlayOutEntityMetadata packet, String context) {
		// get list of objects
		List<WatchableObject> list = (List<WatchableObject>) get(PACKET_LIST, packet);
		// create object mappings of the above list
		HashMap<Integer, WatchableObject> objects = new HashMap<>();
		for (WatchableObject aList : list) {
			objects.put(aList.a(), aList);
		}
		// get entity id
		int id;
		try {
			id = PACKET_ENTITY_ID.getInt(packet);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		// if the map/packet contains an item stack, and is for a console entity
		if (objects.containsKey(8) && isConsoleEntity(id)) {
			// get stack from the map
			ItemStack stack = (ItemStack) objects.get(8).b();
			// global map id
			short global = (short) stack.getData();
			// player context map id
			short translated = translateIndex(context, global);
			// set data of item stack
			if (translated != -1)
				stack.setData(translated);
			else return false;
		}
		// block other map metadata
		else if (objects.containsKey(8) && objects.get(8).b() instanceof ItemStack) {
			ItemStack stack = (ItemStack) objects.get(8).b();
			if (stack.getItem() == Items.FILLED_MAP || stack.getItem() == Items.MAP) {
				return false;
			}
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
		// only remove if this is a standalone server
		if (local)
			clearAllocations(e.getPlayer());
	}
	// block maps (crafting)
	@EventHandler
	public void onItemCraft(CraftItemEvent e) {
		if (e.getRecipe().getResult().getType() == Material.MAP
				|| e.getRecipe().getResult().getType() == Material.EMPTY_MAP)
			e.setCancelled(true);
	}

	// block maps (pick block)
	@EventHandler
	public void onItemCreative(InventoryCreativeEvent e) {
		if (e.getCurrentItem() != null
				&& (e.getCurrentItem().getType() == Material.MAP || e.getCurrentItem().getType() == Material.EMPTY_MAP)
				|| (e.getCursor() != null && (e.getCursor().getType() == Material.MAP
				|| e.getCursor().getType() == Material.EMPTY_MAP)))
			e.setCancelled(true);
	}
	// block frame damage (for any reason)
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof ItemFrame && isConsoleEntity((ItemFrame) e.getEntity()))
			e.setCancelled(true);
	}
	// block frame breaking (for any reason)
	@EventHandler
	public void onFrameBreak(HangingBreakEvent e) {
		if (e.getEntity() instanceof ItemFrame && isConsoleEntity((ItemFrame) e.getEntity())) {
			e.setCancelled(true);
		}
	}
	// block map item pickup
	@EventHandler
	public void onItemPickup(PlayerPickupItemEvent e) {
		if (e.getItem().getItemStack().getType() == Material.MAP
				|| e.getItem().getItemStack().getType() == Material.EMPTY_MAP) {
			e.setCancelled(true);
			e.getItem().remove();
		}
	}
	// block breaking console paintings
	@EventHandler
	public void blockBreaking(BlockBreakEvent e) {
		if (hittingConsole(e.getPlayer()))
			e.setCancelled(true);
	}
	@EventHandler
	public void onPlayerInteractBlock(PlayerInteractAtEntityEvent e) {
		if (e.getRightClicked() instanceof ItemFrame && isConsoleEntity((ItemFrame) e.getRightClicked()))
			e.setCancelled(true);
		clickEvent(e.getPlayer(), null);
	}
	@EventHandler
	public void onPlayerInteract(PlayerInteractEntityEvent e) {
		if (e.getRightClicked() instanceof ItemFrame && isConsoleEntity((ItemFrame) e.getRightClicked()))
			e.setCancelled(true);
		clickEvent(e.getPlayer(), null);
	}
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		clickEvent(e.getPlayer(), e::setCancelled);
	}
	public ManagedConsole[] getConsolesLookingAt(Location eye) {
		return consoles.stream().filter(console -> console.intersect(eye, 7) != null)
				.toArray(ManagedConsole[]::new);
	}
	public boolean hittingConsole(Player player) {
		for (ManagedConsole console : consoles.toArray(new ManagedConsole[consoles.size()])) {
			if (console.created()) {
				int[] arr = console.intersect(player.getEyeLocation(), 7);
				if (arr != null)
					return true;
			}
		}
		return false;
	}
	private void clickEvent(Player player, Consumer<Boolean> cancel) {
		boolean cancelled = false;
		for (ManagedConsole console : consoles.toArray(new ManagedConsole[consoles.size()])) {
			if (console.created()) {
				int[] arr = console.intersect(player.getEyeLocation(), 7);
				if (arr != null) {
					if (!cancelled && cancel != null) {
						cancel.accept(true);
						cancelled = true;
					}
					console.handleClick(arr[0], arr[1], player);
					ComputerHandler handler = ComputerHandler.getInstance();
					if (handler != null) {
						handler.interact(new Position2D(arr[0], arr[1]), player, console);
					}
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
	public short allocate(int size) {
		short lowest = Consoles.startingId;
		synchronized (ALLOCATION_LOCK) {
			while (!fits(lowest, size))
				lowest++;
			for (short t = lowest; t < lowest + size; t++) {
				// update context allocations
				for (String context : allocations.keySet()) {
					if (getIndexTable(context).containsKey(t))
						Consoles.getInstance().getLogger().warning("Overwriting map index allocation for context: "
								+ context);
					mapIndex(context, t);
				}
				defaultAllocation.add(t);
			}
		}
		return lowest;
	}
	public void free(short index, int size) {
		synchronized (ALLOCATION_LOCK) {
			for (short t = index; t < index + size; t++) {
				for (Map<Short, Short> map : allocations.values()) {
					map.remove(t);
				}
				defaultAllocation.remove(Short.valueOf(t));
			}
		}
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
