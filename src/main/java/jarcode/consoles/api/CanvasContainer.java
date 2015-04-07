package jarcode.consoles.api;

import jarcode.consoles.Position2D;
import org.bukkit.entity.Player;

public interface CanvasContainer {
	public Position2D getUnderlingComponentCoordinates(CanvasComponent component);
	public void onClick(int x, int y, Player player);
	public void add(CanvasComponent component);
}
