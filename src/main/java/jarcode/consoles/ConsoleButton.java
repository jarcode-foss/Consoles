package jarcode.consoles;

import jarcode.consoles.event.ButtonEvent;
import jarcode.consoles.event.ConsoleEventListener;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.map.MinecraftFont;

import java.util.ArrayList;
import java.util.List;

public class ConsoleButton extends ConsoleComponent
		implements ListenableComponent<ConsoleButton, ButtonEvent> {
	
	private boolean toggle = false;
	private String text = ChatColor.BLACK + "Button";
	private byte background = 32;
	private byte activeBackground = 36;
	private byte border = 48;
	private ChatColor def = ChatColor.BLACK;
	private List<ConsoleEventListener<ConsoleButton, ButtonEvent>> list = new ArrayList<>();
	private boolean toggleable = false;
	private ConsoleButton(int w, int h, ConsoleRenderer renderer) {
		super(w, h, renderer);
	}

	public ConsoleButton(ConsoleRenderer renderer, String text) {
		this(MinecraftFont.Font.getWidth(ChatColor.stripColor(text)) + 10,
				MinecraftFont.Font.getHeight() + 6, renderer);
		setText(text);
	}
	public void setToggleable(boolean toggleable) {
		this.toggleable = toggleable;
	}
	public void setText(String text) {
		if (!text.startsWith("\u00A7"))
			this.text = def + text;
		else
			this.text = text;
	}
	@Override
	public void setBackground(byte b) {
		background = b;
	}
	private void setActiveBackground(byte b) {
		activeBackground = b;
	}
	public void setBorder(byte b) {
		border = b;
	}

	public void setDefaultChatColor(ChatColor color) {
		def = color;
	}
	
	@Override
	public void paint(ConsoleGraphics g, String context) {
		for (int i = 0; i < getWidth(); i++) {
			for (int j = 0; j < getHeight(); j++) {
				g.draw(i, j, toggle ? activeBackground : background);
			}
			g.draw(i, 0, border);
			g.draw(i, getHeight() - 1, border);
			if (i == 0 || i == getWidth() - 1) for (int j = 0; j < getHeight(); j++) {
				g.draw(i, j, border);
			}
		}
		int w = MinecraftFont.Font.getWidth(ChatColor.stripColor(text));
		int h = MinecraftFont.Font.getHeight();
		g.drawFormatted((getWidth() / 2) - (w / 2), (getHeight() / 2) - (h / 2), text);
	}
	@Override
	public void handleClick(int x, int y, Player player) {
		if (!toggle || toggleable) {
			toggle = !toggleable || !toggle;
			player.getWorld().playSound(player.getLocation(), Sound.CLICK, 1.5f, 1f);
			if (!toggleable)
				doLater(() -> {
					toggle = false;
					repaint();
				}, 8);
			for (ConsoleEventListener<ConsoleButton, ButtonEvent> listener : list) {
				try {
					listener.actionPerformed(new ButtonEvent(this, toggle));
				} catch (Throwable e) {
					Consoles.getInstance().getLogger().severe("Console button listener threw an exception:");
					e.printStackTrace();
				}
			}
			repaint();
		}
	}
	@Override
	public void addEventListener(ConsoleEventListener<ConsoleButton, ButtonEvent> listener) {
		list.add(listener);
	}
}
