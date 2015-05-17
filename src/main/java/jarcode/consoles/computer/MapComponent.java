package jarcode.consoles.computer;

import jarcode.consoles.ConsoleComponent;
import jarcode.consoles.Position2D;
import jarcode.consoles.api.CanvasGraphics;
import jarcode.consoles.util.ChunkMapper;
import jarcode.consoles.util.InstanceListener;
import net.minecraft.server.v1_8_R2.World;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class MapComponent extends ConsoleComponent {

	private static final int ZOOM = 1;

	private final int originX, originZ;
	private final World world;

	private final InstanceListener listener = new InstanceListener();

	private final ChunkMapper.PreparedMapSection sections[] = new ChunkMapper.PreparedMapSection[6];

	{
		for (int t = 0; t < sections.length; t++)
			sections[t] = new ChunkMapper.PreparedMapSection();
	}

	public MapComponent(int w, int h, Computer computer, int centerX, int centerZ) {
		super(w, h, computer.getConsole());
		Position2D corner = ChunkMapper.align(centerX, centerZ, ZOOM);
		this.originX = corner.getX();
		this.originZ = corner.getY();
		this.world = ((CraftWorld) computer.getConsole().getLocation().getWorld()).getHandle();

		// I love generics! This is so nice to write.
		listener.chain(this::trigger)
				.register(BlockBreakEvent.class)
				.register(BlockPlaceEvent.class);
		mapAll();
	}

	public void trigger(BlockEvent e) {
		if (update(e.getBlock().getX(), e.getBlock().getY())) repaint();
	}
	private void mapAll() {
		int off = (64 * (ZOOM + 1));
		for (int x = 0; x < 3; x++)
			for (int y = 0; y < 3; y++)
				update(off + originX + (x * (128 * (ZOOM + 1))), off + originZ + (y * (128 * (ZOOM + 1))));
	}
	private boolean update(int x, int y) {
		boolean update = false;
		for (int xo = 0; xo < 3; xo++)
			for (int yo = 0; yo < 2; yo++)
				update = ChunkMapper.updateSection(sections[xo + (yo * 3)], world,
						originX + (xo * (128 * (ZOOM + 1))), originZ + (yo * (128 * (ZOOM + 1))), x, y, ZOOM);
		return update;
	}

	@Override
	public void handleClick(int x, int y, Player player) {
		int ux = originX + (x * (ZOOM + 1));
		int uz = originZ + (y * (ZOOM + 1));
		player.sendMessage("Updating: " + ux + ", " + uz + " (origin: " + originX + ", " + originZ + ")");

		if (update(ux, uz)) repaint();
	}

	@Override
	public void onRemove() {
		listener.destroy();
	}

	@Override
	public void paint(CanvasGraphics g, String context) {
		for (int xo = 0; xo < 3; xo++)
			for (int yo = 0; yo < 2; yo++)
				sections[xo + (yo * 3)].render(g, xo * 128, yo * 128);
	}
}
