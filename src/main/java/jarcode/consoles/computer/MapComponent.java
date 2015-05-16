package jarcode.consoles.computer;

import jarcode.consoles.ConsoleComponent;
import jarcode.consoles.api.CanvasGraphics;
import jarcode.consoles.util.ChunkMapper;
import jarcode.consoles.util.InstanceListener;
import net.minecraft.server.v1_8_R2.World;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class MapComponent extends ConsoleComponent {

	private final int centerX, centerZ;
	private final World world;

	private final InstanceListener listener = new InstanceListener();

	private final ChunkMapper.PreparedMapSection sections[] = new ChunkMapper.PreparedMapSection[6];

	{
		for (int t = 0; t < sections.length; t++)
			sections[t] = new ChunkMapper.PreparedMapSection();
	}

	public MapComponent(int w, int h, Computer computer, int centerX, int centerZ) {
		super(w, h, computer.getConsole());
		this.centerX = centerX - 200;
		this.centerZ = centerZ - 200;
		this.world = ((CraftWorld) computer.getConsole().getLocation().getWorld()).getHandle();

		// I love generics! This is so nice to write.
		listener.chain(this::trigger)
				.register(PlayerJoinEvent.class)
				.register(BlockBreakEvent.class)
				.register(BlockPlaceEvent.class);
	}

	public void trigger(PlayerEvent e) {
		int x = e.getPlayer().getLocation().getBlockX();
		int z = e.getPlayer().getLocation().getBlockZ();
		boolean update = false;
		for (int xo = 0; xo < 3; xo ++)
			for (int yo = 0; yo < 2; yo++)
				update = ChunkMapper.updateSection(sections[xo + (yo * 3)], world,
						centerX + (xo * 128), centerZ + (yo * 128), x, z, 1);
		if (update) repaint();
	}

	@Override
	public void onRemove() {
		listener.destroy();
	}

	@Override
	public void paint(CanvasGraphics g, String context) {
		for (int xo = 0; xo < 3; xo ++)
			for (int yo = 0; yo < 2; yo++)
				sections[xo + (yo * 3)].render(g, xo * 128, yo * 128);
	}
}
