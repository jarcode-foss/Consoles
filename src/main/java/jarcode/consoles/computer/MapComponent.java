package jarcode.consoles.computer;

import jarcode.consoles.ConsoleComponent;
import jarcode.consoles.Consoles;
import jarcode.consoles.InputComponent;
import jarcode.consoles.Position2D;
import jarcode.consoles.api.CanvasGraphics;
import jarcode.consoles.event.bukkit.MapUpdateEvent;
import jarcode.consoles.util.Allocation;
import jarcode.consoles.util.ChunkMapper;
import jarcode.consoles.util.InstanceListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MapComponent extends ConsoleComponent implements InputComponent {

	// this is the cross for the 'compass' image for the map program
	// it's a dirty way of storing the data, but it works. Sue me.
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

	private static final int REPAINT_DELAY = 200;

	private static final int BAR_HEIGHT = 20;

	private static final MapFont FONT = MinecraftFont.Font;

	private static final String INFO = ChatColor.GREEN + "Input a number"
			+ ChatColor.WHITE + " to change scale, " + ChatColor.GREEN + "/q" + ChatColor.WHITE + " to quit";

	private final int startX, startZ;
	private final World world;

	private final Computer computer;

	private final int tty;

	private final InstanceListener listener = new InstanceListener();

	private final List<String> renderBar = new CopyOnWriteArrayList<>();

	private final List<String> resetBuffer = new CopyOnWriteArrayList<>();

	private final AtomicInteger currentView = new AtomicInteger(0);

	private InfiniteMapView[] views = new InfiniteMapView[4];

	private long lastRepaint = 0;

	private int lastRepaintTask = -1;

	public MapComponent(int w, int h, Computer computer, int centerX, int centerZ, int tty) {
		super(w, h, computer.getConsole());

		this.computer = computer;
		this.tty = tty;

		this.startX = centerX - OFFSET_RATIO_X;
		this.startZ = centerZ - OFFSET_RATIO_Y;

		this.world = computer.getConsole().getLocation().getWorld();

		MapDataStore[] stores = MapDataStore.getStores(world);

		for (int t = 0; t < views.length; t++)
			views[t] = new InfiniteMapView(stores[t],
					startX, startZ, w, h - (BAR_HEIGHT + 2));

		listener.register(MapUpdateEvent.class, this::handleUpdate);

		update(computer.getConsole().getLocation().getBlockX(), computer.getConsole().getLocation().getBlockZ());
	}
	private void handleUpdate(MapUpdateEvent event) {
		if (event.getWorld() == world && currentView.get() == event.getScale()) {
			if (System.currentTimeMillis() - lastRepaint > REPAINT_DELAY) {
				lastRepaint = System.currentTimeMillis();
				views[event.getScale()].onUpdate(event.getX(), event.getZ());
			}
			else if (lastRepaintTask != -1) {
				lastRepaintTask = Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
					handleUpdate(event);
					lastRepaintTask = -1;
				}, REPAINT_DELAY);
			}
		}
	}

	private void update(int x, int z) {
		MapDataStore.update(world, x, z);
	}

	@Override
	public void handleClick(int x, int y, Player player) {
		for (InfiniteMapView view : views) {
			view.handleClick(x, y);
		}
		repaint();
	}

	@Override
	public void onRemove() {
		listener.destroy();
	}

	public void quit() {
		computer.switchView(1);
		computer.setComponent(tty, null);
	}

	@Override
	public void paint(CanvasGraphics g, String context) {
		InfiniteMapView view = views[currentView.get()];
		view.render(g, context);
		int wl = FONT.getChar('W').getWidth() + 1;
		for (int x = 0; x < CROSS_DATA[0].length; x++)
			for (int y = 0; y < CROSS_DATA.length; y++)
				if (CROSS_DATA[x][y] == 1)
					g.draw(x + CROSS_MARGIN + wl, y + CROSS_MARGIN + FONT.getHeight(), CROSS_COLOR);
		g.draw(CROSS_MARGIN + wl + 3, CROSS_MARGIN, CROSS_COLOR, "N");
		g.draw(CROSS_MARGIN + wl + 3, CROSS_MARGIN + CROSS_DATA.length + FONT.getHeight() + 1, CROSS_COLOR, "S");
		g.draw(CROSS_MARGIN + CROSS_DATA[0].length + wl + 1, CROSS_MARGIN + 10, CROSS_COLOR, "E");
		g.draw(CROSS_MARGIN, CROSS_MARGIN + 10, CROSS_COLOR, "W");

		if (!renderBar.contains(context)) {
			renderBar.add(context);
			g.drawBackground(0, getHeight() - BAR_HEIGHT, getWidth(), BAR_HEIGHT);
			for (int x = 0; x < g.getWidth(); x++) {
				g.draw(x, getHeight() - (2 + BAR_HEIGHT), (byte) 44);
				g.draw(x, getHeight() - (1 + BAR_HEIGHT), (byte) 47);
			}
			int textY = getHeight() - BAR_HEIGHT + FONT.getHeight() - 1;
			g.drawFormatted(2, textY, INFO);
			g.drawFormatted(26 + FONT.getWidth(ChatColor.stripColor(INFO)), textY, ChatColor.WHITE
					+ "scale: " + ChatColor.RED + currentView.get() + ChatColor.WHITE + " pos: " + views[currentView.get()].status());
		}
	}

	@Override
	public void handleInput(String input, String player) {
		if (input.equalsIgnoreCase("-q") || input.equalsIgnoreCase("q"))
			quit();
		try {
			int scale = Integer.parseInt(input);
			if (scale >= 0 && scale < views.length) {
				currentView.set(scale);
				renderBar.clear();
				resetBuffer.clear();
				for (InfiniteMapView view : views) {
					view.view.x = startX;
					view.view.z = startZ;
				}
				repaint();
			}
		}
		catch (NumberFormatException ignored) {}
	}

	private class InfiniteMapView {

		// the screen-space view
		private Allocation view;

		// scale of this view
		private final int scale;

		private final int sectionSize;

		private final int windowWidth, windowHeight;

		private final MapDataStore store;

		public InfiniteMapView(MapDataStore store, int viewX, int viewZ, int allocationWidth, int allocationHeight) {
			this.scale = store.scale;
			view = new Allocation(viewX, viewZ, allocationWidth << scale, allocationHeight << scale);
			sectionSize = 128 << scale;
			windowWidth = allocationWidth;
			windowHeight = allocationHeight;
			this.store = store;

		}

		public void onUpdate(int x, int y) {
			if (view.clone().shrink(-128).inside(x, y))
				repaint();
		}

		public void handleClick(int x, int y) {
			// transform into global coordinates
			int dz = (y << scale) - (view.d / 2);
			int dx = (x << scale) - (view.w / 2);
			dz /= 3;
			dx /= 3;
			move(view.x + dx, view.z + dz);
		}

		public String status() {
			return ChatColor.WHITE + "(" + ChatColor.YELLOW + (view.x + (view.w / 2))
					+ ChatColor.WHITE + ", " + ChatColor.YELLOW + (view.z + (view.d / 2))
					+ ChatColor.WHITE + ")";
		}

		public void render(CanvasGraphics g, String context) {
			if (!resetBuffer.contains(context)) {
				g.drawBackground(0, 0, windowWidth, windowHeight);
				resetBuffer.add(context);
			}
			List<Allocation> list = store.map.entrySet().stream()
					.filter(entry -> new Allocation(entry.getKey().getX(),
							entry.getKey().getY(), sectionSize, sectionSize).overlap(view))
					.peek(entry -> render(g, entry.getValue(),
							(entry.getKey().getX() - view.x) >> scale,
							(entry.getKey().getY() - view.z) >> scale,
							(point) -> point.getY() < windowHeight && point.getX() < windowWidth))
					.map(entry -> new Allocation((entry.getKey().getX() - view.x) >> scale,
							(entry.getKey().getY() - view.z) >> scale, sectionSize, sectionSize))
					.collect(Collectors.toList());
			// this resets the pixels in areas that used to have data, but now
			// no longer have a map section (which would normally result in
			// ghosting from the previous frame).
			for (int i = 0; i < windowWidth; i++) {
				for (int j = 0; j < windowHeight; j++) {
					boolean flag = true;
					for (Allocation alloc : list)
						if (alloc.inside(i, j))
							flag = false;
					if (flag)
						g.draw(i, j, (byte) 119);
				}
			}

		}
		public void move(int x, int y) {
			view = new Allocation(x, y, view.w, view.d);
			renderBar.clear();
		}

		public void render(CanvasGraphics g, ChunkMapper.PreparedMapSection section,
		                   int x, int y, Predicate<Position2D> filter) {
			for (int i = 0; i < 128; i++) {
				for (int j = 0; j < 128; j++) {
					if (i + x >= 0 && j + y >= 0 && g.getHeight() > j + y
							&& g.getWidth() > i + x && filter.test(new Position2D(i + x, j + y))) {
						if (section.colors[i + (j * 128)] != 0) {
							if (g.sample(i + x, j + y) != section.colors[i + (j * 128)]) {
								g.draw(i + x, j + y, section.colors[i + (j * 128)]);
							}
						}
						else
							g.draw(i + x, j + y, (byte) 119);
					}
				}
			}
		}
	}
}
