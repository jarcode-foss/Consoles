package jarcode.consoles;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class ConsoleContainer extends ConsoleComponent {

	protected static int maxHeightOf(ConsoleComponent[] list) {
		int max = 0;
		for (ConsoleComponent component : list) {
			if (max < component.getHeight())
				max = component.getHeight();
		}
		return max;
	}


	protected static int totalWidthOf(ConsoleComponent[] list, int margin) {
		int width = 0;
		for (int t = 0; t < list.length; t++) {
			width += list[t].getWidth();
			if (t != list.length - 1)
				width += margin;
		}
		return width;
	}

	protected List<ConsoleComponent> contained = new ArrayList<>();

	public ConsoleContainer(int w, int h, ConsoleRenderer renderer) {
		super(w, h, renderer);
	}
	public ConsoleContainer(int w, int h, ConsoleRenderer renderer, boolean enabled) {
		super(w, h, renderer, enabled);
	}
	public void add(ConsoleComponent component) {
		component.setContained(true);
		contained.add(component);
	}
	protected final int totalContainedWidth(int margin) {
		int width = 0;
		for (int t = 0; t < contained.size(); t++) {
			ConsoleComponent component = contained.get(t);
			width += component.getWidth();
			if (t != contained.size() - 1)
				width += margin;

		}
		return width;
	}
	protected final int maxContainedHeight() {
		int max = 0;
		for (ConsoleComponent component : contained) {
			if (max < component.getHeight())
				max = component.getHeight();
		}
		return max;
	}
	@Override
	protected final void handleClick(int x, int y, Player player) {
		contained.stream().filter(ConsoleComponent::enabled).forEach(comp -> {
			Position2D pos = getUnderlingComponentCoordinates(comp);
			if (pos != null && x >= pos.getX() && y >= pos.getY()
					&& x < pos.getX() + comp.getWidth() && y < pos.getY() + comp.getHeight()) {
				comp.handleClick(x - pos.getX(), y - pos.getY(), player);
			}
		});
		onClick(x, y, player);
	}
	protected abstract void onClick(int x, int y, Player player);
	protected abstract Position2D getUnderlingComponentCoordinates(ConsoleComponent component);
}
