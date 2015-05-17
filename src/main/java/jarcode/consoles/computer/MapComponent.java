package jarcode.consoles.computer;

import jarcode.consoles.ConsoleComponent;
import jarcode.consoles.Position2D;
import jarcode.consoles.api.CanvasGraphics;
import jarcode.consoles.util.Allocation;
import jarcode.consoles.util.ChunkMapper;
import jarcode.consoles.util.InstanceListener;
import jarcode.consoles.util.LocalPosition;
import net.minecraft.server.v1_8_R2.World;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MapComponent extends ConsoleComponent {

	private static final byte[][] CROSS_DATA = {
			{0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0},
			{1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1},
			{0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0},
	};

	private static final byte CROSS_COLOR = 32;

	private static final int CROSS_MARGIN = 10;

	private static final int OFFSET_RATIO_X = 140;
	private static final int OFFSET_RATIO_Y = 80;

	private static final MapFont FONT = MinecraftFont.Font;

	private final int originX, originZ;
	private final World world;

	private final InstanceListener listener = new InstanceListener();

	private InfiniteMapView[] views = new InfiniteMapView[4];

	private AtomicInteger currentView = new AtomicInteger(0);

	public MapComponent(int w, int h, Computer computer, int centerX, int centerZ) {
		super(w, h, computer.getConsole());

		this.originX = centerX - OFFSET_RATIO_X;
		this.originZ = centerZ - OFFSET_RATIO_Y;

		for (int t = 0; t < views.length; t++)
			views[t] = new InfiniteMapView(originX, originZ, w, h, t);

		this.world = ((CraftWorld) computer.getConsole().getLocation().getWorld()).getHandle();

		// I love generics! This is so nice to write.
		listener.chain(this::trigger)
				.register(BlockBreakEvent.class)
				.register(BlockPlaceEvent.class);
	}

	public void trigger(BlockEvent e) {
		for (InfiniteMapView view : views)
			if (view.update(e.getBlock().getX(), e.getBlock().getY()) && currentView.get() == view.scale)
				repaint();
	}
	private boolean update(int x, int y) {
		boolean flag = false;
		for (InfiniteMapView view : views)
			flag = view.update(x, y);
		return flag;
	}

	@Override
	public void handleClick(int x, int y, Player player) {
		player.sendMessage("origin: " + originX + ", " + originZ);
		for (InfiniteMapView view : views) {
			view.handleClick(x, y);
			Position2D pos = transform(x, y, view.scale);
			player.sendMessage("Updating: " + pos.getX() + ", " + pos.getY() + " (scale: " + view.scale + ")");
			if (view.update(pos.getX(), pos.getY()) && currentView.get() == view.scale)
				repaint();
		}
	}

	private Position2D transform(int x, int y, int scale) {
		int ux = originX + (x * (1 << scale)) - (64 * (1 << scale));
		int uz = originZ + (y * (1 << scale)) - (64 * (1 << scale));
		return new Position2D(ux, uz);
	}

	@Override
	public void onRemove() {
		listener.destroy();
	}

	@Override
	public void paint(CanvasGraphics g, String context) {
		InfiniteMapView view = views[currentView.get()];
		view.render(g);
		int wl = FONT.getChar('W').getWidth() + 1;
		for (int x = 0; x < CROSS_DATA[0].length; x++)
			for (int y = 0; y < CROSS_DATA.length; y++)
				if (CROSS_DATA[x][y] == 1)
					g.draw(x + CROSS_MARGIN + wl, y + CROSS_MARGIN + FONT.getHeight(), CROSS_COLOR);
		g.draw(CROSS_MARGIN + wl + 3, CROSS_MARGIN, CROSS_COLOR, "N");
		g.draw(CROSS_MARGIN + wl + 3, CROSS_MARGIN + CROSS_DATA.length + FONT.getHeight() + 1, CROSS_COLOR, "S");
		g.draw(CROSS_MARGIN + CROSS_DATA[0].length + wl + 1, CROSS_MARGIN + 10, CROSS_COLOR, "E");
		g.draw(CROSS_MARGIN, CROSS_MARGIN + 10, CROSS_COLOR, "W");
	}

	private class InfiniteMapView {

		private static final int MAX_VIEW_SIZE = 16;

		private Allocation view;
		private HashMap<Position2D, ChunkMapper.PreparedMapSection> map = new HashMap<>();
		private final int scale;
		private final int originX, originY;

		public InfiniteMapView(int originX, int originY, int allocationWidth, int allocationHeight, int zoom) {
			view = new Allocation(originX, originY, allocationWidth, allocationHeight);
			this.scale = zoom;
			this.originX = originX;
			this.originY = originY;
		}
		public void collect() {
			while (map.size() > 16) {
				Position2D pos = map.keySet().stream()
						.filter(key -> !(new Allocation(key.getX(), key.getY(), 128, 128).overlap(view)))
						.findFirst()
						.orElseGet(() -> null);
				if (pos != null) {
					System.out.println("removing section: " + pos.getX() + ", " + pos.getY());
					map.remove(pos);
				}
			}
		}
		// view-space coordinates
		public void handleClick(int x, int y) {
			int uz = view.z + (y * (1 << scale)) - (64 * (1 << scale));
			int ux = view.x + (x * (1 << scale)) - (64 * (1 << scale));
			update(ux, uz);
		}

		// global coordinates
		public ChunkMapper.PreparedMapSection getAt(int x, int y) {
			Map.Entry<Position2D, ChunkMapper.PreparedMapSection> ret = map.entrySet().stream()
					.filter(entry -> new Allocation(entry.getKey().getX(),
							entry.getKey().getY(), 128, 128).inside(new LocalPosition(x, 0, y)))
					.findFirst()
					.orElseGet(() -> null);
			return ret == null ? null : ret.getValue();
		}

		// global coordinates
		public Map.Entry<Position2D, ChunkMapper.PreparedMapSection> createAt(int x, int y) {
			int xo = (x - view.x) >> 7; // divide by 128 and truncate
			int yo = (y - view.z) >> 7;
			Position2D corner = new Position2D(view.x + (xo * (128 * (1 << scale))), view.z + (yo * (128 * (1 << scale))));
			ChunkMapper.PreparedMapSection section = new ChunkMapper.PreparedMapSection();
			map.put(corner, section);
			return new AbstractMap.SimpleEntry<>(corner, section);
		}

		// global coordinates
		public boolean update(int x, int y) {
			if (getAt(x, y) == null) {
				Map.Entry<Position2D, ChunkMapper.PreparedMapSection> entry = createAt(x, y);
				System.out.println("creating section at: " + entry.getKey().getX() + ", " + entry.getKey().getY());
				ChunkMapper.updateSection(entry.getValue(), world,
						entry.getKey().getX(), entry.getKey().getY(),
						x, y, scale);
				return true;
			}
			AtomicBoolean bool = new AtomicBoolean();
			map.entrySet().stream()
					.filter(entry -> new Allocation(entry.getKey().getX(),
							entry.getKey().getY(), 128, 128).overlap(view))
					.forEach(entry -> {
						if (ChunkMapper.updateSection(entry.getValue(), world,
								entry.getKey().getX(), entry.getKey().getY(),
								x, y, scale))
							bool.set(true);
					});
			return bool.get();
		}
		public ChunkMapper.PreparedMapSection[] viewableSections() {
			return map.entrySet().stream()
					.filter(entry -> new Allocation(entry.getKey().getX(),
							entry.getKey().getY(), 128, 128).overlap(view))
					.map(Map.Entry::getValue)
					.toArray(ChunkMapper.PreparedMapSection[]::new);
		}

		public void render(CanvasGraphics g) {
			map.entrySet().stream()
					.filter(entry -> new Allocation(entry.getKey().getX(),
							entry.getKey().getY(), 128, 128).overlap(view))
					.peek(entry -> System.out.println("rendering at view-space coordinates: "
							+ (entry.getKey().getX() - view.x) + ", " + (entry.getKey().getY() - view.z)))
					.forEach(entry -> entry.getValue()
							.render(g, entry.getKey().getX() - view.x, entry.getKey().getY() - view.z));
		}
		public void move(int x, int y) {
			view = new Allocation(x, y, view.w, view.d);
			collect();
		}
	}
}
