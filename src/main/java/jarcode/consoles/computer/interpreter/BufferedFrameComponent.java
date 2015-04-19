package jarcode.consoles.computer.interpreter;

import jarcode.consoles.ConsoleComponent;
import jarcode.consoles.InputComponent;
import jarcode.consoles.api.CanvasGraphics;
import jarcode.consoles.computer.Computer;
import jarcode.consoles.computer.interpreter.types.LuaInteraction;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class BufferedFrameComponent extends ConsoleComponent implements InputComponent {

	private CopyOnWriteArrayList<LuaInteraction> interactions = new CopyOnWriteArrayList<>();
	private final Object STR_LOCK = new Object();
	private StringBuilder input = new StringBuilder();
	private final CopyOnWriteArrayList<Consumer<CanvasGraphics>> ops = new CopyOnWriteArrayList<>();

	public BufferedFrameComponent(Computer computer) {
		super(computer.getViewWidth(), computer.getViewHeight(), computer.getConsole());
		this.setEnabled(true);
	}

	public void addOperations(Collection<Consumer<CanvasGraphics>> ops) {
		ops.addAll(ops);
		repaint();
	}

	public LuaInteraction interaction() {
		if (interactions.size() == 0) return null;
		LuaInteraction[] arr = interactions.stream()
				.limit(interactions.size() - 1)
				.toArray(LuaInteraction[]::new);
		interactions.clear();
		interactions.addAll(Arrays.asList(arr));
		return arr[arr.length - 1];
	}

	public String input() {
		synchronized (STR_LOCK) {
			String str = input.toString();
			input = new StringBuilder();
			return str;
		}
	}

	@Override
	public void handleClick(int x, int y, Player player) {
		interactions.add(new LuaInteraction(x, y, player.getName()));
	}

	@Override
	public void paint(CanvasGraphics g, String context) {
		for (Consumer<CanvasGraphics> op : ops) {
			op.accept(g);
		}
		ops.clear();
	}

	@Override
	public void handleInput(String input, String player) {
		synchronized (STR_LOCK) {
			this.input.append(input);
			this.input.append('\n');
		}
	}
}
