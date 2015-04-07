package jarcode.consoles;

import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;

public abstract class ConsoleComponent {
	private byte bg = -1;
	private int w, h;
	private boolean enabled;
	private ConsoleRenderer renderer;
	private boolean contained = false;
	public ConsoleComponent(int w, int h, ConsoleRenderer renderer) {
		this.w = w;
		this.h = h;
		this.renderer = renderer;
		enabled = true;
	}
	public ConsoleComponent(int w, int h, ConsoleRenderer renderer, boolean enabled) {
		this.w = w;
		this.h = h;
		this.renderer = renderer;
		this.enabled = enabled;
	}
	public void setContained(boolean contained) {
		this.contained = contained;
	}
	public void setEnabled(boolean enabled) {
		if (enabled != this.enabled) {
			this.enabled = enabled;
			renderer.cacheBackground();
		}
	}
	public boolean enabled() {
		return enabled;
	}
	public int getWidth() {
		return w;
	}
	public int getHeight() {
		return h;
	}
	public boolean isContained() {
		return contained;
	}
	public byte getBackground() {
		return bg;
	}
	public void setBackground(byte bg) {
		this.bg = bg;
	}
	public ConsoleRenderer getRenderer() {
		return renderer;
	}
	public final void link(CommandBlock block) {
		if (this instanceof WritableComponent) {
			ConsoleHandler.registerListener(block, ((WritableComponent) this).createListener());
		}
		else throw new IllegalArgumentException("Component must implement " + WritableComponent.class.getSimpleName());
	}
	protected void repaint() {
		renderer.repaint();
	}
	protected void handleClick(int x, int y, Player player) {}
	public void onRemove() {}
	protected void doLater(Runnable runnable, long delay) {
		renderer.doLater(runnable, delay);
	}
	// this is async. Don't be stupid and ignore synchronizing this with your fields.
	public abstract void paint(ConsoleGraphics g, String context);
}
